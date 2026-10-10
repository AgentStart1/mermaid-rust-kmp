# Building and verification

Use Rust stable, JDK 21, Python 3.12 and BoltFFI CLI 0.31.0. The Rust dependency remains
BoltFFI 0.31 (Cargo.lock resolves 0.31.0); the CLI and Wasm runtime use 0.31.0 too.
Windows needs Visual Studio C++ build tools and clang-cl; macOS needs Xcode tools.
Android/Wasm builds additionally need Android SDK/NDK, Node/npm and Binaryen; the Wasm wrapper installs its TypeScript dependencies.

```sh
cargo install boltffi_cli --version 0.31.0 --locked
python scripts/build-desktop-jni.py
bash sample/gradlew -p desktop-test check jvmJar --console=plain
python -m unittest discover -s scripts -p 'test_*.py'
```

BoltFFI 0.31.0's experimental KMP emitter rejects String parameters. Instead,
`scripts/generate-kmp.py` uses its supported Kotlin/JNI emitter with `--deny-skipped`,
then assembles the KMP project from `templates/kmp`. Edit those templates rather than
generated dist files. The common API and JVM facade filename remain compatible;
generated JNI bindings live in the existing jvm package and are shared with Android.
JNI glue and headers are generated together by the same generator.

For a full Android/Wasm build:

```sh
python scripts/install-binaryen.py
# Add the printed bin directory to PATH before running boltffi pack wasm.
python scripts/generate-kmp.py
cd mermaid-ffi
boltffi pack android --release --deny-skipped
python ../scripts/build-wasm.py
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

`ci.yml` and `publish.yml` each build the four desktop targets on their respective
hosts. CI builds and runs all three public API rendering tests in the same job
on each host, using the locally generated Kotlin sources and JNI library. CI does
not upload or download Actions artifacts; the Android/Wasm build runs separately.
On version tags, the publishing workflow uploads only native resources from each
host, merges them and publishes to Maven Central/npm without repeating tests.
`scripts/check-desktop-package.py <jar>` validates a complete four-platform release
JAR; it is not invoked on the single-platform JARs built by CI.
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

BoltFFI 0.31.0 includes the generated Node bindings in its npm file list. The
runtime version is pinned in boltffi.toml and in Kotlin/Wasm dependencies.
`scripts/build-wasm.py` installs matching local TypeScript, Node types and runtime
dependencies before packing: 0.31 validates TypeScript strictly in a staging folder.
CI runs all three APIs against flowchart and sequence diagrams using
`node scripts/test-wasm-rendering.mjs`.

`scripts/install-binaryen.py` downloads official Binaryen 133 and verifies its
release SHA-256. CI adds its bin directory to PATH automatically. The crates.io
wasm-opt 0.116 tool does not understand BoltFFI 0.31.0's --enable-bulk-memory-opt.

The generated publishing project uses AGP 8.13.2 and its own checksum-pinned Gradle
8.13 wrapper, compatible with Kotlin 2.3.21. Desktop tests keep the sample wrapper.
Use the generated project's wrapper for its build; do not invoke the sample wrapper
against the Android publishing project.

Release desktop artifacts contain only host-specific JNI resources. Each CI host
generates its own Kotlin sources for its rendering tests. The publishing job also
generates its own sources and merges only native libraries, avoiding concurrent
extraction of duplicate Kotlin files across LF/CRLF checkouts.

Desktop macOS builds use a fixed 15.0 deployment target for both architectures.
The build passes MACOSX_DEPLOYMENT_TARGET to Rust and the binding generator,
and -mmacosx-version-min=15.0 to the JNI compiler/linker. It verifies the final
dylib deployment version with otool after building. Builds and
rendering tests run on macOS 15, matching the minimum supported version.
