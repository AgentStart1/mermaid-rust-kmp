"""Assemble the stable KMP facade around BoltFFI's supported Kotlin/JNI generator."""
import argparse
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CRATE = ROOT / "mermaid-ffi"
DIST = CRATE / "dist/kotlin-multiplatform"

def generate(boltffi="boltffi"):
    subprocess.run([sys.executable, str(ROOT / "scripts/prepare-renderer.py")], check=True)
    subprocess.run([boltffi, "generate", "kotlin", "--deny-skipped"], cwd=CRATE, check=True)
    templates = ROOT / "templates/kmp"
    for name in ("build.gradle.kts", "settings.gradle.kts"):
        DIST.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(templates / name, DIST / name)
    for platform, name in (("common", "MermaidFfi.kt"), ("jvm", "MermaidFfiJvmActual.kt"), ("android", "MermaidFfiAndroidActual.kt")):
        kotlin = DIST / f"src/{platform}Main/kotlin"
        kotlin.resolve().relative_to(DIST.resolve())
        if kotlin.exists():
            shutil.rmtree(kotlin)
        package = kotlin / "com/storyteller_f/mermaid_kmp"
        package.mkdir(parents=True)
        shutil.copyfile(templates / name, package / name)
        if platform != "common":
            shutil.copytree(CRATE / "dist/kotlin-jni/com", kotlin / "com", dirs_exist_ok=True)
    c_dir = DIST / "src/jvmMain/c"
    shutil.copytree(CRATE / "dist/kotlin-jni/jni", c_dir, dirs_exist_ok=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--boltffi", default="boltffi")
    generate(parser.parse_args().boltffi)
