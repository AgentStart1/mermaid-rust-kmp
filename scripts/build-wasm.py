"""Build the optimized Wasm package with the generator's matching type dependencies."""
import argparse
import os
from pathlib import Path
import platform
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CRATE = ROOT / "mermaid-ffi"

def build(boltffi):
    subprocess.run([sys.executable, str(ROOT / "scripts/prepare-renderer.py")], check=True)
    npm = ["cmd", "/d", "/c", "npm"] if platform.system() == "Windows" else ["npm"]
    subprocess.run([*npm, "install", "--prefix", str(CRATE / "dist/wasm"), "--no-save", "--package-lock=false", "typescript@5.9.3", "@types/node@22.19.0", "@boltffi/runtime@0.32.0"], cwd=CRATE, check=True)
    environment = os.environ.copy()
    environment["PATH"] = str(CRATE / "dist/wasm/node_modules/.bin") + os.pathsep + environment.get("PATH", "")
    subprocess.run([boltffi, "pack", "wasm", "--release", "--deny-skipped"], cwd=CRATE, env=environment, check=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--boltffi", default="boltffi")
    build(parser.parse_args().boltffi)
