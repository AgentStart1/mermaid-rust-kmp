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
PR CI runs for build sources, scripts, templates, shared Actions and ci.yml changes.
Changes confined to publish.yml or documentation do not trigger the build/test jobs.
On version tags, the publishing workflow uploads only native resources from each
host, merges them and publishes to Maven Central/npm without repeating tests.
`scripts/check-desktop-package.py <jar>` validates a complete four-platform release
JAR; it is not invoked on the single-platform JARs built by CI.
Pushing a `v<version>` tag starts publication. Do not overwrite existing releases:
0.0.1-alpha2 contains only Linux x86_64 and this fix requires a new version.

Prerelease versions (for example, 0.0.1-alpha4) publish to the npm `next` channel;
stable versions publish to `latest`. The workflow explicitly passes `--tag` for
both, as npm 11 requires a tag when publishing prereleases. Build metadata does
not affect channel selection. The version itself remains unchanged.

## Release authentication

npm publishes `mermaid-ffi-wasm` using GitHub Actions OIDC, without NPM_TOKEN or
NODE_AUTH_TOKEN. The publish job runs only in `storytellerF/mermaid-rust-kmp` on a
GitHub-hosted Ubuntu runner and grants `id-token: write` only to that job. Node
is constrained to 22.14.0 or newer within Node 22; npm is explicitly installed at
11.21.0 (Trusted Publishing requires npm >=11.5.1). setup-node does not generate
an auth-token .npmrc; the public registry is passed explicitly to npm. Generated
package metadata is updated before publication with the upstream Git repository
URL required for provenance. Existing Rust, BoltFFI, npm and Gradle caches remain.

Before a release, a package maintainer must open
[the npm package settings](https://www.npmjs.com/package/mermaid-ffi-wasm/access)
and add a GitHub Actions Trusted Publisher with these exact, case-sensitive fields:

| Field | Value |
| --- | --- |
| Package | `mermaid-ffi-wasm` |
| Organization or user | `storytellerF` |
| Repository | `mermaid-rust-kmp` |
| Workflow filename | `publish.yml` (not its full path) |
| Environment name | Leave blank; the job declares no GitHub environment |
| Allowed actions | Enable direct `npm publish` |

The package already exists (0.0.1-alpha2); no bootstrap publication is needed.
Current npm settings default new connections to staged publishing, so explicitly
allow direct publication to match this workflow. A new connection must complete
its first successful publish within two days; recreate it if it expires. If a
GitHub environment is added later, update the npm binding to match exactly.
After a successful OIDC release, remove the unused GitHub NPM_TOKEN secret and
revoke its npm token; consider npm's "Require two-factor authentication and disallow
tokens" setting. Never put tokens in source or logs.

The v0.0.1-alpha3 run failed at npm with E404 on the registry PUT. This does not
prove token expiry: package access/authentication also needs verification. All
four native builds succeeded and Maven publication was skipped, so this run
provides no evidence of a Maven authentication failure. Do not rerun the old tag
expecting new workflow code; release a new, unpublished version after merging and
configuring the binding, checking npm and Central for existing versions first.

Maven Central has independent authentication. Its current official Portal API
documents Portal user tokens, not npm-style GitHub OIDC Trusted Publishing.
Keep CENTRAL_USERNAME and CENTRAL_PASSWORD as the two components of a Portal
user token, plus GPG_PRIVATE_KEY and GPG_PASSPHRASE for artifact signing. These
are not the interactive account password or legacy OSSRH credentials. No Maven
credentials are rotated by this migration; verify them separately if that step
fails in a future release.

References: [npm Trusted Publishing](https://docs.npmjs.com/trusted-publishers/),
[npm trust prerequisites](https://docs.npmjs.com/cli/v11/commands/npm-trust/), and
[Central Portal API authentication](https://central.sonatype.org/publish/publish-portal-api/).

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

## Actions dependency caches

Both workflows use `.github/actions/build-cache` after installing Rust stable.
Swatinem/rust-cache 2.9.2 caches the Cargo registry/git dependencies and
`mermaid-ffi/target`, with separate OS, runner architecture and desktop versus
Android/Wasm target-set keys. Its compiler fingerprint and Cargo manifest/lock
hashes further isolate toolchains and dependencies. Explicit source hashes cover
the renderer timing patch and JNI build configuration. The checksum-verified
renderer path patch is prepared before the action runs Cargo metadata; generated
JNI resources are rebuilt locally rather than restored as CI artifacts.

BoltFFI CLI 0.31.0 uses an independent actions/cache 6.1.0 entry keyed by OS,
architecture and the complete rustc toolchain identity. Only the isolated
`runner.temp/boltffi-cli/bin` is cached, never `~/.cargo/bin` or Rust tools.
Every restore checks `boltffi --version`; matching binaries skip cargo install,
and missing or mismatched binaries are installed with the exact locked version.
Step summaries report Rust and CLI exact cache hits.

Gradle User Home, dependencies, wrapper distributions and local build state use
only `gradle/actions/setup-gradle` 6.4.0 with its enhanced cache provider.
setup-java supplies JDK 21 without caching. Setup runs after generated projects
and their patches are ready, before the first Gradle invocation; automatic wrapper
validation checks the available wrapper JARs. No Gradle version is installed by
the action: desktop tests and generated KMP builds retain their own wrappers.
The action keys state by OS, job, workflow/matrix and commit, separating the four
desktop hosts (including the two macOS architectures) and Android/Wasm jobs.
It restores the closest available state and deduplicates shared dependencies.
`cache-read-only: false` allows CI to save PR merge-ref caches and publishing to
save tag caches; PR entries can be reused by subsequent runs of that PR, but are
not accessible to release tags. The post-action stops Gradle daemons before
cleanup and saving, including on Windows, so no separate `--stop` is required.
Do not add setup-java caching or an actions/cache entry for Gradle User Home.
Gradle cache restore/save details are recorded in action logs and job summaries.

Android/Wasm and publishing use Node 22 and cache only npm's `~/.npm` download
cache. There is no committed npm lockfile: the Wasm staging install pins its tool
and runtime versions in `scripts/build-wasm.py`, and its package is generated.
The key hashes that script, BoltFFI configuration, patch script and KMP templates;
a same-platform Node 22 fallback reuses verified downloads. Installs still resolve
and validate dependencies on every run; node_modules and generated packages are
not cached. setup-node's automatic package-manager caching is disabled to avoid
an overlapping provider. PR caches are scoped to the PR merge ref, so a rerun of
the same PR can verify warm hits; release tags cannot consume a PR-only cache.

Provider references: [Rust cache inputs and keying](https://github.com/Swatinem/rust-cache),
[official dependency cache](https://github.com/actions/cache), and
[setup-gradle configuration and caching](https://github.com/gradle/actions/blob/v6.4.0/docs/setup-gradle.md).
