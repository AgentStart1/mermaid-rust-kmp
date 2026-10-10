# Building and verification

Use Rust stable, JDK 21, Python 3 and BoltFFI CLI 0.30.1. The Rust dependency remains
BoltFFI 0.30 (Cargo.lock resolves 0.30.1); the CLI and Wasm runtime use 0.30.1 too.
Windows needs Visual Studio C++ build tools and clang-cl; macOS needs Xcode tools.
Android/Wasm builds additionally need Android SDK/NDK, Node/npm, TypeScript and wasm-opt.

```sh
cargo install boltffi_cli --version 0.30.1 --locked
python scripts/build-desktop-jni.py
bash sample/gradlew -p desktop-test check jvmJar --console=plain
python -m unittest discover -s scripts -p 'test_*.py'
```

BoltFFI 0.30.1's experimental KMP emitter rejects String parameters. Instead,
`scripts/generate-kmp.py` uses its supported Kotlin/JNI emitter with `--deny-skipped`,
then assembles the KMP project from `templates/kmp`. Edit those templates rather than
generated dist files. The common API and JVM facade filename remain compatible;
generated JNI bindings live in the existing jvm package and are shared with Android.
JNI glue and headers are generated together by the same generator.

For a full Android/Wasm build:

```sh
python scripts/install-binaryen.py
# Add the printed bin directory to PATH before running boltffi pack wasm.
npm install -g typescript
python scripts/generate-kmp.py
cd mermaid-ffi
boltffi pack android --release --deny-skipped
boltffi pack wasm --release
cd ..
python scripts/build-desktop-jni.py
./patch-ffi.sh
cd mermaid-ffi/dist/kotlin-multiplatform
./gradlew build -Pversion=0.0.0-local -Pgroup=io.github.storytellerf
```

`patch-ffi.sh [npm-version]` adds publishing and Wasm integration. Without a version,
it uses the local generated npm package. The desktop tests exercise all three public
functions and assert SVG output and node labels. Native libraries load from bundled
`native/<host>` resources without configuring java.library.path.

`build-native.yml` only builds the four desktop targets on their respective hosts.
`ci.yml` downloads and merges those artifacts, runs rendering tests on each host and
checks that the JVM JAR contains all four nonempty JNI libraries, alongside the
Android/Wasm build. `publish.yml` reuses the pure build workflow on version tags,
merges its artifacts and publishes to Maven Central/npm without repeating tests.
Pushing a `v<version>` tag starts publication. Do not overwrite existing releases:
0.0.1-alpha2 contains only Linux x86_64 and this fix requires a new version.

## Renderer Wasm timing patch

The crates.io renderer 0.3.1 calls std::time::Instant during layout, which panics
on bare wasm32-unknown-unknown. `scripts/prepare-renderer.py` stages the exact 0.3.1
crate under ignored `build/renderer`, verifying its original crates.io checksum,
and replaces three clock imports with `templates/renderer/time.rs`. Native builds
still use std::time::Instant; Wasm diagnostic timings are zero. Layout and rendering
logic are unchanged. This avoids wasm-bindgen clock imports in BoltFFI's bare Wasm.
`generate-kmp.py` prepares this patched dependency automatically. Before running
Cargo directly from a clean checkout, run `python scripts/prepare-renderer.py`.
The renderer [upstream timing fix](https://github.com/1jehuang/mermaid-rs-renderer/pull/155)
is still unmerged; remove the local patch when an appropriate release is available.

BoltFFI 0.30.1's npm file list omits its generated Node binding; patch-ffi.sh adds
that binding and pins @boltffi/runtime to 0.30.1. CI runs the three real Wasm APIs
against flowchart and sequence diagrams with `node scripts/test-wasm-rendering.mjs`.

`scripts/install-binaryen.py` downloads official Binaryen 133 and verifies its
release SHA-256. CI adds its bin directory to PATH automatically. The crates.io
wasm-opt 0.116 tool does not understand BoltFFI 0.30.1's --enable-bulk-memory-opt.
