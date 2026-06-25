#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$SCRIPT_DIR/mermaid-ffi/dist/kotlin-multiplatform"
WASM_PKG_DIR="$SCRIPT_DIR/mermaid-ffi/dist/wasm/pkg"

# Optional: pass a version to use a published npm package instead of file: reference
# Usage: ./patch-ffi.sh [npm-version]
# Example: ./patch-ffi.sh 1.0.0
NPM_VERSION="${1:-}"

if [ -n "$NPM_VERSION" ]; then
    WASM_NPM_DEP="implementation(npm(\"mermaid-ffi-wasm\", \"$NPM_VERSION\"))"
else
    WASM_NPM_DEP='implementation(npm("mermaid-ffi-wasm", "file:../wasm/pkg"))'
fi

if [ ! -d "$DIST_DIR" ]; then
    echo "Error: dist directory not found: $DIST_DIR"
    exit 1
fi

# 1. Copy Gradle wrapper from sample/
cp "$SCRIPT_DIR/sample/gradlew" "$DIST_DIR/gradlew"
cp "$SCRIPT_DIR/sample/gradlew.bat" "$DIST_DIR/gradlew.bat"
chmod +x "$DIST_DIR/gradlew"
mkdir -p "$DIST_DIR/gradle/wrapper"
cp "$SCRIPT_DIR/sample/gradle/wrapper/gradle-wrapper.jar" "$DIST_DIR/gradle/wrapper/"
cp "$SCRIPT_DIR/sample/gradle/wrapper/gradle-wrapper.properties" "$DIST_DIR/gradle/wrapper/"

# 2. Patch settings.gradle.kts for wasmJs Node.js repository compatibility
SETTINGS_FILE="$DIST_DIR/settings.gradle.kts"
sed -i '/repositoriesMode\.set/d' "$SETTINGS_FILE"

# 3. Patch build.gradle.kts (idempotent)
BUILD_FILE="$DIST_DIR/build.gradle.kts"

if grep -q 'vanniktech' "$BUILD_FILE"; then
    echo "Already patched, skipping."
    exit 0
fi

sed -i 's/plugins {/plugins {\n    id("com.vanniktech.maven.publish") version "0.30.0"/' "$BUILD_FILE"

cat >> "$BUILD_FILE" <<GRADLE

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(npm("@boltffi/runtime", "0.26.1"))
                $WASM_NPM_DEP
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set("mermaid-ffi-kmp")
        description.set("Kotlin Multiplatform bindings for mermaid-rs-renderer via BoltFFI")
        url.set("https://github.com/storytellerF/mermaid-rust-kmp")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("storytellerF")
                name.set("storytellerF")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/storytellerF/mermaid-rust-kmp.git")
            developerConnection.set("scm:git:ssh://github.com/storytellerF/mermaid-rust-kmp.git")
            url.set("https://github.com/storytellerF/mermaid-rust-kmp")
        }
    }
}
GRADLE

# 4. Create wasmJs actual implementations
if [ -d "$WASM_PKG_DIR" ]; then
    WASM_SRC_DIR="$DIST_DIR/src/wasmJsMain/kotlin/com/storyteller_f/mermaid_kmp"
    mkdir -p "$WASM_SRC_DIR/wasm"

    cat > "$WASM_SRC_DIR/wasm/MermaidFfiExternals.kt" <<'KOTLIN'
@file:JsModule("mermaid-ffi-wasm")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.mermaid_kmp.wasm

import kotlin.js.ExperimentalWasmJsInterop

external fun renderMermaid(input: String): String

external fun renderMermaidWithSpacing(input: String, nodeSpacing: Float, rankSpacing: Float): String

external fun renderMermaidClassicTheme(input: String): String
KOTLIN

    cat > "$WASM_SRC_DIR/MermaidFfiWasmJsActual.kt" <<'KOTLIN'
package com.storyteller_f.mermaid_kmp

import com.storyteller_f.mermaid_kmp.wasm.renderMermaid as wasmRenderMermaid
import com.storyteller_f.mermaid_kmp.wasm.renderMermaidWithSpacing as wasmRenderMermaidWithSpacing
import com.storyteller_f.mermaid_kmp.wasm.renderMermaidClassicTheme as wasmRenderMermaidClassicTheme

actual fun renderMermaid(input: String): String =
    wasmRenderMermaid(input)

actual fun renderMermaidWithSpacing(input: String, nodeSpacing: Float, rankSpacing: Float): String =
    wasmRenderMermaidWithSpacing(input, nodeSpacing, rankSpacing)

actual fun renderMermaidClassicTheme(input: String): String =
    wasmRenderMermaidClassicTheme(input)
KOTLIN

    echo "wasmJs sources created."
else
    echo "Warning: WASM package not found at $WASM_PKG_DIR, skipping wasmJs sources."
fi

echo "Patched."
