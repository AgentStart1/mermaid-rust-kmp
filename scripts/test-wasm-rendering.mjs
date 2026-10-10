import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

const pkg = resolve(process.argv[2] ?? "mermaid-ffi/dist/wasm/pkg");
const api = await import(pathToFileURL(resolve(pkg, "node.js")));
await api.initialized;
for (const input of ["graph TD; A[Alpha]-->B[Beta]", "sequenceDiagram\n participant A as Alpha\n participant B as Beta\n A->>B: Hello"]) {
    for (const [name, args] of [
        ["renderMermaid", [input]],
        ["renderMermaidWithSpacing", [input, 50, 70]],
        ["renderMermaidClassicTheme", [input]],
    ]) {
        const svg = api[name](...args);
        assert.match(svg, /<svg[\s>]/, name);
        assert.match(svg, /Alpha/, name);
        assert.match(svg, /Beta/, name);
    }
}
console.log("Wasm: three rendering APIs passed for flowchart and sequence diagrams");
