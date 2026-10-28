"""Install the fixed official Binaryen tool required by BoltFFI 0.32.0."""
import hashlib
import io
import os
from pathlib import Path
import platform
import tarfile
import urllib.request

VERSION = "133"
ROOT = Path(__file__).resolve().parents[1] / "build/binaryen"
system = {"Windows": "windows", "Linux": "linux", "Darwin": "macos"}[platform.system()]
machine = platform.machine().lower()
architecture = "x86_64" if machine in ("x86_64", "amd64") else ("aarch64" if system == "linux" else "arm64")
archive_name = f"binaryen-version_{VERSION}-{architecture}-{system}.tar.gz"
directory = ROOT / f"binaryen-version_{VERSION}"
executable = directory / "bin" / ("wasm-opt.exe" if system == "windows" else "wasm-opt")
if not executable.is_file():
    base = f"https://github.com/WebAssembly/binaryen/releases/download/version_{VERSION}/{archive_name}"
    def download(url):
        with urllib.request.urlopen(url, timeout=60) as response:
            return response.read()
    expected = download(base + ".sha256").decode().split()[0]
    archive = download(base)
    if hashlib.sha256(archive).hexdigest() != expected:
        raise RuntimeError("Binaryen archive checksum mismatch")
    ROOT.mkdir(parents=True, exist_ok=True)
    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as tar:
        for member in tar.getmembers():
            (ROOT / member.name).resolve().relative_to(ROOT.resolve())
        tar.extractall(ROOT, filter="data")
if not executable.is_file():
    raise RuntimeError(f"Binaryen executable missing: {executable}")
binary_path = str(executable.parent)
if "GITHUB_PATH" in os.environ:
    with open(os.environ["GITHUB_PATH"], "a") as github_path:
        github_path.write(binary_path + "\n")
print(binary_path)
