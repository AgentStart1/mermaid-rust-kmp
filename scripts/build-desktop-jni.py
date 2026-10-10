"""Build the current host JNI library without rebuilding Android or Wasm."""
import argparse
import os
from pathlib import Path
import platform
import re
import shlex
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CRATE = ROOT / "mermaid-ffi"
DIST = CRATE / "dist/kotlin-multiplatform"
MACOS_DEPLOYMENT_TARGET = "11.0"


def run(args, **kwargs):
    result = subprocess.run(args, cwd=CRATE, **kwargs)
    if result.returncode and kwargs.get("capture_output"):
        sys.stderr.write(result.stderr)
    result.check_returncode()
    return result


def build(boltffi):
    system = platform.system()
    machine = platform.machine().lower()
    if system == "Windows":
        if machine not in ("amd64", "x86_64"):
            raise RuntimeError("Windows build requires x86_64")
        vswhere = Path(os.environ["ProgramFiles(x86)"]) / "Microsoft Visual Studio/Installer/vswhere.exe"
        installation = subprocess.check_output([str(vswhere), "-latest", "-products", "*", "-requires", "Microsoft.VisualStudio.Component.VC.Tools.x86.x64", "-property", "installationPath"], text=True).strip()
        if not installation:
            raise RuntimeError("Install Visual Studio C++ build tools")
        devcmd = Path(installation) / "Common7/Tools/VsDevCmd.bat"
        env_output = subprocess.check_output(f'cmd.exe /d /s /c ""{devcmd}" -arch=x64 -host_arch=x64 >nul && set"', text=True)
        for line in env_output.splitlines():
            key, separator, value = line.partition("=")
            if separator and key:
                os.environ[key] = value
        compiler = shutil.which("clang-cl") or str(Path(os.environ["ProgramFiles"]) / "LLVM/bin/clang-cl.exe")
        host, filename, jni_platform = "windows-x86_64", "mermaid_ffi_jni.dll", "win32"
    elif system == "Darwin":
        host = "darwin-aarch64" if machine == "arm64" else "darwin-x86_64"
        compiler, filename, jni_platform = "clang", "libmermaid_ffi_jni.dylib", "darwin"
    elif system == "Linux" and machine == "x86_64":
        compiler, host, filename, jni_platform = "clang", "linux-x86_64", "libmermaid_ffi_jni.so", "linux"
    else:
        raise RuntimeError(f"Unsupported host: {system}/{machine}")
    environment = os.environ.copy()
    if system == "Darwin":
        environment["MACOSX_DEPLOYMENT_TARGET"] = MACOS_DEPLOYMENT_TARGET
    subprocess.run([sys.executable, str(ROOT / "scripts/generate-kmp.py"), "--boltffi", boltffi], env=environment, check=True)
    c_dir = DIST / "src/jvmMain/c"
    result = run(["cargo", "rustc", "--release", "--locked", "--lib", "--", "--print", "native-static-libs"], env=environment, capture_output=True, text=True, encoding="utf-8", errors="replace")
    match = re.search(r"native-static-libs: (.+)", result.stderr)
    if not match:
        raise RuntimeError("Rust did not report native link dependencies")
    libraries = shlex.split(match.group(1))
    java_include = Path(os.environ["JAVA_HOME"]) / "include"
    output = DIST / "src/jvmMain/resources/native" / host / filename
    output.parent.mkdir(parents=True, exist_ok=True)
    includes = [c_dir, java_include, java_include / jni_platform]
    glue = str(c_dir / "jni_glue.c")
    if system == "Windows":
        args = [compiler, "/LD", "/MD", "/O2", "/clang:-std=c11", *[f"/I{path}" for path in includes], glue, str(CRATE / "target/release/mermaid_ffi.lib"), f"/Fe:{output}", "/link", *libraries]
    else:
        args = [compiler, "-dynamiclib" if system == "Darwin" else "-shared", "-fPIC", "-O2", "-std=c11", *[f"-I{path}" for path in includes], glue, str(CRATE / "target/release/libmermaid_ffi.a"), *libraries, "-o", str(output)]
    if system == "Darwin":
        args.insert(1, f"-mmacosx-version-min={MACOS_DEPLOYMENT_TARGET}")
    run(args, env=environment)
    if not output.is_file():
        raise RuntimeError(f"JNI output missing: {output}")
    if system == "Darwin":
        load_commands = subprocess.check_output(["otool", "-l", str(output)], text=True)
        versions = []
        for command in re.split(r"Load command \d+\n", load_commands):
            if re.search(r"\bcmd LC_(?:BUILD_VERSION|VERSION_MIN_MACOSX)\b", command):
                match = re.search(r"^\s+(?:minos|version) (\d+\.\d+(?:\.\d+)?)\s*$", command, re.MULTILINE)
                if not match:
                    raise RuntimeError(f"Missing macOS deployment version in {output}")
                versions.append(match.group(1))
        if not versions or any(tuple(map(int, version.split(".")))[:2] != (11, 0) for version in versions):
            raise RuntimeError(f"Unexpected macOS deployment version in {output}: {versions}")
        print(f"macOS deployment target verified: {MACOS_DEPLOYMENT_TARGET}")
    print(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--boltffi", default="boltffi")
    build(parser.parse_args().boltffi)
