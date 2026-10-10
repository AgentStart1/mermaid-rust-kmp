"""Correct BoltFFI 0.30.1's npm file list for its generated Node entrypoint."""
import json
from pathlib import Path

PACKAGE = Path(__file__).resolve().parents[1] / "mermaid-ffi/dist/wasm/pkg"
manifest = PACKAGE / "package.json"
if manifest.exists():
    metadata = json.loads(manifest.read_text())
    node_entry = PACKAGE / "mermaid_ffi_node.js"
    if not node_entry.is_file():
        raise RuntimeError(f"Generated Node bindings missing: {node_entry}")
    for filename in ("mermaid_ffi_node.js", "mermaid_ffi_node.d.ts"):
        if filename not in metadata["files"]:
            metadata["files"].append(filename)
    metadata["dependencies"]["@boltffi/runtime"] = "0.30.1"
    manifest.write_text(json.dumps(metadata, indent=2) + "\n")
