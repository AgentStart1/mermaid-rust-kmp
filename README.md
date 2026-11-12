# mermaid-rust-kmp

`mermaid-rust-kmp` 为 [mermaid-rs-renderer](https://crates.io/crates/mermaid-rs-renderer) 提供 Kotlin Multiplatform 绑定。在 Android 和 JVM 上通过 Rust 原生库，在 Kotlin/Wasm 上通过 WebAssembly，将 Mermaid 文本渲染为 SVG 字符串。

## 支持的平台

| 平台 | 状态 |
| --- | --- |
| Android | 已支持 |
| JVM / Desktop | Linux x86_64、Windows x86_64、macOS 15+（Intel 和 Apple Silicon） |
| Kotlin/Wasm 浏览器 | 已支持 |
| Kotlin/JS | 尚不支持原生渲染 |
| iOS | 尚不支持原生渲染 |

## 添加依赖

已发布的 Kotlin Multiplatform 库坐标：

```kotlin
implementation("io.github.storytellerf:mermaidffi-kmp:<version>")
```

将依赖添加到每个需要渲染图表的 source set。例如，Android 和 JVM 可以共用一个 source set：

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

Kotlin/Wasm 目标还需要与库一起发布的 npm 包：

```kotlin
wasmJsMain.dependencies {
    implementation("io.github.storytellerf:mermaidffi-kmp:<version>")
    implementation(npm("mermaid-ffi-wasm", "<version>"))
}
```

## 使用

从 `com.storyteller_f.mermaid_kmp` 导入函数，传入 Mermaid 语法，即可获得 SVG 文本。

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

可用 API：

- `renderMermaid(input)`：使用默认选项渲染。
- `renderMermaidWithSpacing(input, nodeSpacing, rankSpacing)`：调整节点间距和层级间距。
- `renderMermaidClassicTheme(input)`：使用 Mermaid 默认的 classic 主题渲染。

底层渲染器无法处理输入时，调用会抛出异常。在 UI 或服务边界捕获异常，并向用户提供有帮助的错误信息。

## 示例应用

[`sample/`](./sample) 是 Compose Multiplatform 应用，包含 Android、Desktop、Web（JS/Wasm）和 iOS 入口，提供 Mermaid 编辑器并显示生成的 SVG 文本。

```sh
cd sample

# Android APK
./gradlew :androidApp:assembleDebug

# Desktop application
./gradlew :desktopApp:run

# Kotlin/Wasm browser application
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

示例的 iOS 和 Kotlin/JS 目标显示“Native rendering not yet supported”提示。Wasm、Android 和 JVM 目标会调用本库。

示例界面默认跟随系统或宿主的浅色、深色主题，切换时保留编辑内容和渲染结果。主题仅影响控件、页面背景与 SVG 源码文字，不修改生成的 SVG 或 Mermaid 图形主题。

桌面原生库打包与验证见 [DEVELOPMENT.md](./DEVELOPMENT.md)。

`0.0.1-alpha2` 仅包含 Linux x86_64 原生库。Windows 和 macOS 打包修复需要发布新版本。

源码构建、验证和发布说明见 [DEVELOPMENT.md](./DEVELOPMENT.md)。
