"""Stage the checksum-pinned renderer with a bare-Wasm diagnostic-clock patch."""
import hashlib
import io
import os
from pathlib import Path
import tarfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
NAME = "mermaid-rs-renderer-0.3.1"
CHECKSUM = "6cb5b469222ce6b9896edf0a969cf68464f5a028ef394fd6643f00bd943df117"

def prepare():
    template = (ROOT / "templates/renderer/time.rs").read_bytes()
    stamp = CHECKSUM + hashlib.sha256(template).hexdigest()
    stage = ROOT / "build/renderer"
    source = stage / NAME
    marker = source / ".mermaid-wasm-patch"
    if marker.exists() and marker.read_text() == stamp:
        return
    cargo_home = Path(os.environ.get("CARGO_HOME", Path.home() / ".cargo"))
    archives = list((cargo_home / "registry/cache").glob(f"*/{NAME}.crate"))
    if archives:
        archive = archives[0].read_bytes()
    else:
        with urllib.request.urlopen("https://static.crates.io/crates/mermaid-rs-renderer/mermaid-rs-renderer-0.3.1.crate", timeout=60) as response:
            archive = response.read()
    if hashlib.sha256(archive).hexdigest() != CHECKSUM:
        raise RuntimeError("Renderer archive differs from the pinned crates.io checksum")
    stage.mkdir(parents=True, exist_ok=True)
    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as tar:
        for member in tar.getmembers():
            destination = (stage / member.name).resolve()
            destination.relative_to(source.resolve())
        tar.extractall(stage, filter="data")
    for relative in ("src/lib.rs", "src/layout/mod.rs", "src/layout/flowchart/edge_pipeline.rs"):
        path = source / relative
        contents = path.read_text()
        if contents.count("use std::time::Instant;") != 1:
            raise RuntimeError(f"Unexpected renderer clock import: {relative}")
        contents = contents.replace("use std::time::Instant;", "use crate::time::Instant;")
        if relative == "src/lib.rs":
            contents += "\nmod time;\n"
        path.write_text(contents)
    (source / "src/time.rs").write_bytes(template)
    marker.write_text(stamp)

if __name__ == "__main__":
    prepare()
