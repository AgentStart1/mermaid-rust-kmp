# mermaid-rust-kmp

`mermaid-rust-kmp` provides Kotlin Multiplatform bindings for [mermaid-rs-renderer](https://crates.io/crates/mermaid-rs-renderer). It renders Mermaid text to SVG strings through native Rust libraries on Android and JVM, or WebAssembly on Kotlin/Wasm.

## Supported platforms

| Platform | Status |
| --- | --- |
| Android | Supported |
| JVM / Desktop | Supported |
| Kotlin/Wasm browser | Supported |
| Kotlin/JS | Native rendering is not yet supported |
| iOS | Native rendering is not yet supported |

## Add the dependency

The published Kotlin Multiplatform library coordinates are:

```kotlin
implementation("io.github.storytellerf:mermaidffi-kmp:<version>")
```

Add the dependency to each source set that renders diagrams. For example, Android and JVM can share a source set:

```kotlin
kotlin {
    sourceSets {
        val jvmAndAndroidMain by creating {
            dependencies {
                implementation("io.github.storytellerf:mermaidffi-kmp:<version>")
            }
        }
    }
}
```

Kotlin/Wasm targets also need the npm package published alongside the library:

```kotlin
wasmJsMain.dependencies {
    implementation("io.github.storytellerf:mermaidffi-kmp:<version>")
    implementation(npm("mermaid-ffi-wasm", "<version>"))
}
```

## Usage

Import a function from `com.storyteller_f.mermaid_kmp`, pass Mermaid syntax, and receive SVG text.

```kotlin
import com.storyteller_f.mermaid_kmp.renderMermaid

val svg = renderMermaid(
    """flowchart LR
        A[Start] --> B{Decision}
        B -->|Yes| C[OK]
        B -->|No| D[Cancel]
    """.trimIndent(),
)
```

Available APIs:

- `renderMermaid(input)`: renders with the default options.
- `renderMermaidWithSpacing(input, nodeSpacing, rankSpacing)`: adjusts node and rank spacing.
- `renderMermaidClassicTheme(input)`: renders with the Mermaid default (classic) theme.

Calls throw an exception when the underlying renderer cannot process the input. Catch it at your UI or service boundary and present a useful error to the user.

## Sample application

[`sample/`](./sample) is a Compose Multiplatform application with Android, Desktop, Web (JS/Wasm), and iOS entry points. It provides a Mermaid editor and displays the resulting SVG text.

```sh
cd sample

# Android APK
./gradlew :androidApp:assembleDebug

# Desktop application
./gradlew :desktopApp:run

# Kotlin/Wasm browser application
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

The sample's iOS and Kotlin/JS targets display a "Native rendering not yet supported" message. Its Wasm, Android, and JVM targets invoke this library.

## Build from source

Building requires Rust stable, the Android NDK/SDK, JDK 21, Node.js/npm, TypeScript, `wasm-opt`, and BoltFFI CLI 0.26.1. Generate the bindings and build the generated KMP project with:

```sh
cargo install boltffi_cli --version 0.26.1
cargo install wasm-opt
npm install -g typescript

cd mermaid-ffi
boltffi pack kmp --release --experimental
boltffi pack wasm --release
cd ..
./patch-ffi.sh

cd mermaid-ffi/dist/kotlin-multiplatform
./gradlew build -Pversion=0.0.0-local -Pgroup=io.github.storytellerf
```

`patch-ffi.sh` adds Maven publishing configuration, Kotlin/Wasm support, and the Wasm implementation to the generated KMP project. To make the generated project use a published npm package, pass its version: `./patch-ffi.sh <version>`.

## Publishing

Pushing a Git tag in the form `v<version>` triggers GitHub Actions to publish `mermaid-ffi-wasm` to npm and `mermaidffi-kmp` to Maven Central.
