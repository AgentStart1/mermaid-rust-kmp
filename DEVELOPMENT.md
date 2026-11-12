# 构建与验证

## 示例界面主题验证

`sample/shared` 使用系统主题选择 Material 3 浅色或深色配色。根 `Surface` 提供页面背景和正文颜色，SVG 源码区域使用 `surfaceVariant/onSurfaceVariant`，不修改渲染器输出。

```sh
bash sample/gradlew -p sample :shared:jvmTest --console=plain
```

主题测试覆盖普通文字配色的 4.5:1 对比度、浅深色双向重组、编辑内容与 SVG 源码保留、错误提示、首次深色进入和重新进入的页面背景。桌面 UI 测试显式传入主题值，并向成功/错误场景注入确定的渲染结果；它们不验证原生渲染器，也不能替代系统主题事件验证。

Android 的 `MainActivity` 仅声明自行处理 `uiMode`。系统主题切换不重建 Activity，原有组合、编辑/结果状态和渲染协程继续存活；配置仍由 Android 传给 Compose，`isSystemInDarkTheme()` 观察配置并重组。`onConfigurationChanged` 保留父类调用并重新应用 edge-to-edge 系统栏图标对比度。这里没有覆盖旋转等其他配置重建或进程死亡，也不将可能很大的 SVG 写入 saved-instance Bundle。

Android 宿主回归在 `ThemeConfigurationTest`。先独占一台 API 29+ 的测试设备或模拟器，使用自动/浅色/深色模式（自定义定时模式会跳过），再执行：

```sh
bash sample/gradlew -p sample :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.storyteller_f.project.ThemeConfigurationTest \
  --console=plain
```

测试通过 `cmd uimode night` 触发真实系统配置分发，不向 `App` 传主题 Boolean；四个独立场景覆盖编辑、成功结果、异常结果及未完成请求。每次双向切换均检查 Activity 身份、资源配置、Compose 的 `LocalConfiguration` 和页面背景像素，并在结束后恢复系统模式。成功/错误/进行中场景注入确定渲染器，不调用原生解析器；进行中请求使用受控闩锁，检查跨切换只启动一次且最终结果仍发布。执行期间会改变该测试设备的全局主题，因此不可与其他设备任务并行。若执行被强制中止，使用 `adb shell cmd uimode night no` 恢复浅色；测试前为自动/深色时，将 `no` 分别替换为 `auto`/`yes`。

手动补充：在系统设置中双向切换主题，分别检查编辑输入、现有 SVG、错误提示和渲染中的状态；确认页面没有重新出现初始示例。记录 Android 版本、Activity 是否重建及截图。此手动路径也用于覆盖未运行自动化的设备版本。

在 Android、Desktop、Web 和 iOS 宿主上分别检查：以浅色和深色首次打开；保留输入内容，在页面打开时切换系统主题两个方向；离开后切换主题再进入。检查标题、输入文字与标签、按钮的焦点/悬停、输入框边框，以及渲染失败后的错误提示。支持原生渲染的平台还应检查成功后的 SVG 源码区，确认字符串内容未随 UI 主题改变；iOS/JS 应保留其不支持原生渲染的提示。记录宿主与版本，并区分真实系统切换和测试参数注入。

## 工具链与库验证

使用 Rust stable、JDK 21、Python 3.12 和 BoltFFI CLI 0.32.0。Rust 依赖、CLI 和 Wasm 运行时均使用 BoltFFI 0.32（锁定到 0.32.0）。crate 根按 0.32 导出宏的要求调用 `boltffi::scaffolding!()`。Windows 需要 Visual Studio C++ build tools 和 clang-cl；macOS 需要 Xcode 工具。Android/Wasm 构建还需要 Android SDK/NDK、Node/npm 和 Binaryen；Wasm 封装层会安装自身的 TypeScript 依赖。

```sh
cargo install boltffi_cli --version 0.32.0 --locked
python scripts/build-desktop-jni.py
bash sample/gradlew -p desktop-test check jvmJar --console=plain
python -m unittest discover -s scripts -p 'test_*.py'
```

项目使用 Kotlin/JNI 路径：BoltFFI 0.31.0 的实验性 KMP emitter 拒绝 String 参数时引入了这条路径。`scripts/generate-kmp.py` 使用受支持的 Kotlin/JNI emitter，并传入 `--deny-skipped`，再根据 `templates/kmp` 组装 KMP 项目。应编辑这些模板，而非生成的 dist 文件。通用 API 和 JVM 门面文件名保持兼容；生成的 JNI 绑定位于已有 jvm 包中，与 Android 共用。JNI 桥接代码和头文件由同一个生成器一起生成。

完整 Android/Wasm 构建：

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

`patch-ffi.sh [npm-version]` 添加发布和 Wasm 集成。不传版本时，使用本地生成的 npm 包。桌面测试调用全部三个公开函数，并断言 SVG 输出和节点标签。原生库从随包提供的 `native/<host>` 资源加载，无需配置 java.library.path。

`ci.yml` 和 `publish.yml` 都在各自宿主机上构建四个桌面目标。CI 在每台宿主机的同一作业中，使用本地生成的 Kotlin 源码和 JNI 库，构建并运行全部三个公开 API 的渲染测试。CI 不上传或下载 Actions artifacts；Android/Wasm 构建单独运行。PR CI 针对构建源码、脚本、模板、共享 Actions 和 ci.yml 的变化执行。对 main 的 push 使用相同路径过滤器，运行 Linux 桌面测试和 Android/Wasm 构建，生成共享缓存基线。为限制持续预热成本，CI 中 Windows 和两个 macOS 宿主仍只在 PR 上运行。Main 使用与 PR 相同的作业、目标键、绑定生成和渲染检查；不发布包，也不需要发布凭据。同一分支或 PR 的新运行会取消尚未完成的旧运行，避免重复预热和测试。仅涉及 publish.yml 或文档的改动不会触发构建和测试作业。版本 tag 的发布工作流只上传各宿主的原生资源，合并后发布到 Maven Central/npm，不重复测试。`scripts/check-desktop-package.py <jar>` 验证完整的四平台发布 JAR，不用于 CI 构建的单平台 JAR。推送 `v<version>` tag 开始发布。不要覆盖已有版本：0.0.1-alpha2 仅包含 Linux x86_64，此修复需要新版本。

预发布版本（例如 0.0.1-alpha4）发布到 npm 的 `next` 通道，稳定版本发布到 `latest`。工作流为两者显式传入 `--tag`，因为 npm 11 发布预发布版本时要求指定 tag。构建元数据不影响通道选择；版本号本身保持不变。

## 发布认证

npm 通过 GitHub Actions OIDC 发布 `mermaid-ffi-wasm`，不使用 NPM_TOKEN 或 NODE_AUTH_TOKEN。发布作业只在 `storytellerF/mermaid-rust-kmp` 的 GitHub 托管 Ubuntu runner 上执行，仅该作业获得 `id-token: write`。Node 限定在 Node 22 系列的 22.14.0 或更新版本；npm 显式安装 11.21.0（Trusted Publishing 要求 npm >=11.5.1）。setup-node 不生成带 auth-token 的 .npmrc；公开 registry 显式传给 npm。发布前更新生成包的元数据，加入 provenance 所需的上游 Git 仓库 URL。保留已有 Rust、BoltFFI、npm 和 Gradle 缓存。

发布前，包维护者必须打开 [npm 包设置](https://www.npmjs.com/package/mermaid-ffi-wasm/access)，添加 GitHub Actions Trusted Publisher，并精确填写以下区分大小写的字段：

| 字段 | 值 |
| --- | --- |
| Package | `mermaid-ffi-wasm` |
| Organization or user | `storytellerF` |
| Repository | `mermaid-rust-kmp` |
| Workflow filename | `publish.yml`（不含完整路径） |
| Environment name | 留空；作业没有声明 GitHub environment |
| Allowed actions | 启用直接 `npm publish` |

包已经存在（0.0.1-alpha2），不需要首次引导发布。npm 当前默认对新连接启用 staged publishing，因此要显式允许直接发布，以匹配工作流。新连接必须在两天内完成首次成功发布；过期后需重新创建。如果以后添加 GitHub environment，必须同步修改 npm 绑定，精确匹配。OIDC 发布成功后，移除未使用的 GitHub NPM_TOKEN secret，并撤销对应 npm token；可考虑 npm 的“Require two-factor authentication and disallow tokens”设置。不得将令牌写入源码或日志。

v0.0.1-alpha3 运行在 npm registry PUT 时收到 E404。这不能证明令牌过期，还需核查包访问权限和认证。四个原生构建全部成功，Maven 发布被跳过，因此本次运行没有提供 Maven 认证失败的证据。不要重跑旧 tag 并期待使用新工作流代码；合并并配置绑定后，先检查 npm 和 Central 已存在的版本，再发布一个尚未使用的新版本。

Maven Central 使用独立认证。其当前官方 Portal API 文档使用 Portal user token，并非 npm 式的 GitHub OIDC Trusted Publishing。保留 CENTRAL_USERNAME 和 CENTRAL_PASSWORD，分别作为 Portal user token 的两个组成部分；同时保留用于构件签名的 GPG_PRIVATE_KEY 和 GPG_PASSPHRASE。它们不是交互登录密码或旧 OSSRH 凭据。本迁移不会轮换 Maven 凭据；未来发布在该步骤失败时，应单独验证。

参考：[npm Trusted Publishing](https://docs.npmjs.com/trusted-publishers/)、[npm 信任前置要求](https://docs.npmjs.com/cli/v11/commands/npm-trust/)、[Central Portal API 认证](https://central.sonatype.org/publish/publish-portal-api/)。

## 渲染器 Wasm 计时补丁

crates.io 的 renderer 0.3.1 在布局期间调用 std::time::Instant，会在裸 wasm32-unknown-unknown 上 panic。`scripts/prepare-renderer.py` 将准确的 0.3.1 crate 暂存到被忽略的 `build/renderer`，核验其原始 crates.io 校验和，再将三处时钟导入替换为 `templates/renderer/time.rs`。原生构建仍使用 std::time::Instant；Wasm 诊断耗时为零。布局和渲染逻辑不变。这样可避免 BoltFFI 裸 Wasm 引入 wasm-bindgen 时钟导入。`generate-kmp.py` 自动准备打过补丁的依赖。干净检出中直接运行 Cargo 前，先执行 `python scripts/prepare-renderer.py`。渲染器的[上游计时修复](https://github.com/1jehuang/mermaid-rs-renderer/pull/155) 尚未合并；有合适发布版本后移除本地补丁。

BoltFFI 0.32.0 在 npm 文件清单中包含生成的 Node 绑定。运行时版本固定在 boltffi.toml 和 Kotlin/Wasm 依赖中。`scripts/build-wasm.py` 在打包前安装匹配的本地 TypeScript、Node 类型和运行时依赖：0.32 会在暂存目录严格验证 TypeScript。CI 使用 `node scripts/test-wasm-rendering.mjs`，在流程图和时序图上测试全部三个 API。

`scripts/install-binaryen.py` 下载官方 Binaryen 133 并核验发布 SHA-256。CI 自动将其 bin 目录加入 PATH。crates.io 的 wasm-opt 0.116 不识别 BoltFFI 0.32.0 的 --enable-bulk-memory-opt。

生成的发布项目使用 AGP 8.13.2 和独立、固定校验和的 Gradle 8.13 Wrapper，与 Kotlin 2.3.21 兼容。桌面测试继续使用 sample 的 Wrapper。构建生成项目时使用其自身 Wrapper，不要用 sample Wrapper 调用 Android 发布项目。

桌面发布产物只包含各宿主专属 JNI 资源。每个 CI 宿主为自己的渲染测试生成 Kotlin 源码。发布作业也自行生成源码，只合并原生库，避免 LF/CRLF 检出并发提取重复 Kotlin 文件。

macOS 桌面构建对两种架构都固定使用 15.0 部署目标。构建向 Rust 和绑定生成器传入 MACOSX_DEPLOYMENT_TARGET，向 JNI 编译器和链接器传入 -mmacosx-version-min=15.0。构建后通过 otool 验证最终 dylib 的部署版本。构建和渲染测试在 macOS 15 上运行，与最低支持版本一致。

## Actions 依赖缓存

两个工作流都在安装 Rust stable 后使用 `.github/actions/build-cache`。Swatinem/rust-cache 2.9.2 缓存 Cargo registry/git 依赖和 `mermaid-ffi/target`，按操作系统、runner 架构，以及桌面与 Android/Wasm 目标集合分别设置键。其编译器指纹和 Cargo manifest/lock 哈希进一步隔离工具链和依赖。显式源码哈希覆盖渲染器计时补丁及 JNI 构建配置。通过校验和验证的 renderer 路径补丁，在 action 运行 Cargo metadata 之前准备；生成的 JNI 资源在本地重建，不作为 CI artifacts 恢复。

BoltFFI CLI 0.32.0 使用独立的 actions/cache 6.1.0 条目，键包含操作系统、架构和完整 rustc 工具链标识。只缓存隔离的 `runner.temp/boltffi-cli/bin`，不缓存 `~/.cargo/bin` 或 Rust 工具。每次恢复都检查 `boltffi --version`；匹配的二进制跳过 cargo install，缺失或不匹配时按精确锁定版本安装。步骤摘要报告 Rust 和 CLI 是否精确命中缓存。

Gradle User Home、依赖、Wrapper 分发包和本地构建状态只使用 `gradle/actions/setup-gradle` 6.4.0 及其增强缓存提供方。setup-java 提供 JDK 21，不启用缓存。Setup 在生成项目和补丁准备好之后、首次 Gradle 调用之前运行；自动 Wrapper 验证检查可用的 Wrapper JAR。Action 不安装 Gradle 版本：桌面测试和生成的 KMP 构建各自保留自己的 Wrapper。状态键包含操作系统、作业、工作流／矩阵和提交，分开四个桌面宿主（包括两种 macOS 架构）与 Android/Wasm 作业。它恢复最接近的可用状态，并对共享依赖去重。`cache-read-only: false` 允许 CI 保存 PR merge-ref 缓存，发布流程保存 tag 缓存；PR 条目可供同一 PR 后续运行复用，但其他 PR 或发布 tag 无法访问。平台和缓存键兼容时，main 条目可供新 PR 和 tag 使用。Post-action 在清理和保存前停止 Gradle daemon，Windows 也如此，因此不需要单独 `--stop`。不要再为 Gradle User Home 添加 setup-java 缓存或 actions/cache 条目。恢复和保存细节记录在 action 日志与作业摘要中。

Android/Wasm 和发布使用 Node 22，仅缓存 npm 的 `~/.npm` 下载缓存。仓库不提交 npm lockfile：Wasm 暂存安装在 `scripts/build-wasm.py` 中固定工具和运行时版本，其包为生成内容。缓存键对该脚本、BoltFFI 配置、补丁脚本和 KMP 模板计算哈希；同平台 Node 22 的回退键复用已验证下载。每次运行仍解析并验证依赖；不缓存 node_modules 和生成包。setup-node 自动包管理器缓存被关闭，避免提供方重叠。PR 缓存限定于 PR merge ref，因此同一 PR 重跑可验证热命中；新 PR 和发布 tag 则可以从 main 恢复兼容的 Linux 下载。

### 验证 main 缓存基线

在 fork 中启用 Actions，将此 CI 配置推送到其默认 `main` 分支，不覆盖无关提交。初始空缓存可覆盖冷启动路径：两个 Linux 作业都必须生成绑定，并通过构建和渲染检查，随后将缓存保存到 `refs/heads/main`。运行完成后，针对该 fork 的 main 新建 PR。通过日志和缓存列表确认 Rust、BoltFFI 恢复 main 条目，setup-gradle 恢复 main 状态或依赖。Gradle 每提交状态通常通过回退匹配恢复；仅凭精确命中的布尔值不能判断是否实际恢复了内容。同时检查 Android/Wasm 作业的 npm 恢复。不要为了展示命中而清除上游缓存或跳过绑定生成。

比较冷 main 与新 PR 的作业和步骤耗时、恢复及保存的归档大小、缓存传输时间。还应与完整 PR 矩阵比较累计 runner 时间，并与关键路径的实际耗时区分。初始范围是 Linux 桌面加 Android/Wasm，不是完整发布构建。没有测量数据证明额外 runner 成本合理前，不为 main 预热 Windows/macOS；如果持续构建和缓存传输成本超过 PR 节省，也应缩小 Linux 范围。在本次变更的 PR 中记录运行链接和测量数据，供工具链或依赖更新后复核。一对运行只能证明复用，不能保证未来性能。

提供方参考：[Rust 缓存输入与键规则](https://github.com/Swatinem/rust-cache)、[官方依赖缓存](https://github.com/actions/cache)、[setup-gradle 配置与缓存](https://github.com/gradle/actions/blob/v6.4.0/docs/setup-gradle.md)。
