# Desktop native builds

The desktop JVM artifact must contain Linux x86_64, Windows x86_64, macOS Intel,
and macOS Apple Silicon JNI libraries. Each library is built on its own host;
assembling a release on Ubuntu alone does not build Windows or macOS libraries.

Use Rust stable, JDK 21, Python 3, and BoltFFI CLI 0.26.1. Windows additionally
requires Visual Studio C++ build tools and clang-cl. macOS requires Xcode command
line tools. The Rust BoltFFI dependency is pinned to 0.26.1 to match the generator
and Wasm runtime: 0.30 changes the exported ABI, and its experimental KMP generator
does not support the String parameters used by these rendering APIs.

```sh
cargo install boltffi_cli --version 0.26.1 --locked
python scripts/build-desktop-jni.py
bash sample/gradlew -p desktop-test check jvmJar --console=plain
```

The standalone desktop test project compiles the generated common and JVM bindings
without requiring the Android SDK. Its three tests call default, custom spacing,
and classic theme rendering, asserting SVG output and both diagram node labels.
The JVM loader reads JNI libraries from `native/<host>` resources, so consumers do
not need to configure `java.library.path`.

The release workflow runs these checks on Windows and both Mac architectures,
merges their resource artifacts into the generated KMP project, and validates the
resulting JVM JAR together with the Linux library before publishing anything:

```sh
python scripts/check-desktop-package.py desktop-test/build/libs/mermaid-desktop-test-jvm.jar
```

Existing 0.0.1-alpha2 publications contain only the Linux x86_64 library. Ship this
fix with a new version and update consumers; do not overwrite an existing release.
