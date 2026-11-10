这是面向 Android、iOS、Web 和 Desktop（JVM）的 Kotlin Multiplatform 项目。

* [/iosApp](./iosApp/iosApp) 包含 iOS 应用。即使通过 Compose Multiplatform 共享 UI，iOS 应用仍然需要此入口。项目的 SwiftUI 代码也应放在这里。

* [/shared](./shared/src) 存放多个 Compose Multiplatform 应用共用的代码，包含若干子目录：
  - [commonMain](./shared/src/commonMain/kotlin) 存放所有目标共用的代码。
  - 其他目录存放仅针对目录名所示平台编译的 Kotlin 代码。例如，Kotlin 应用的 iOS 部分需要调用 Apple CoreCrypto 时，应放在 [iosMain](./shared/src/iosMain/kotlin)；Desktop（JVM）专用部分则放在 [jvmMain](./shared/src/jvmMain/kotlin)。

### 运行应用

使用 IDE 工具栏运行控件提供的运行配置，也可以使用以下命令和选项：

- Android 应用：`./gradlew :androidApp:assembleDebug`
- Desktop 应用：
  - 热重载：`./gradlew :desktopApp:hotRun --auto`
  - 常规运行：`./gradlew :desktopApp:run`
- Web 应用：
  - Wasm 目标（更快，适用于现代浏览器）：`./gradlew :webApp:wasmJsBrowserDevelopmentRun`
  - JS 目标（较慢，支持旧浏览器）：`./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS 应用：在 Xcode 中打开 [/iosApp](./iosApp) 目录并运行。

### 运行测试

使用 IDE 编辑器边栏中的运行按钮，或通过 Gradle 任务运行测试：

- Android 测试：`./gradlew :shared:testAndroidHostTest`
- Desktop 测试：`./gradlew :shared:jvmTest`
- Web 测试：
  - Wasm 目标：`./gradlew :shared:wasmJsTest`
  - JS 目标：`./gradlew :shared:jsTest`
- iOS 测试：`./gradlew :shared:iosSimulatorArm64Test`

---

进一步了解 [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)、[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)、[Kotlin/Wasm](https://kotl.in/wasm/)……

欢迎在公开 Slack 频道 [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web) 反馈 Compose/Web 和 Kotlin/Wasm 的使用体验。如果遇到问题，请向 [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP) 报告。
