// Native timing stays unchanged. Bare Wasm has no clock; these metrics are diagnostic only.
#[cfg(not(target_arch = "wasm32"))]
pub(crate) use std::time::Instant;

#[cfg(target_arch = "wasm32")]
pub(crate) struct Instant;

#[cfg(target_arch = "wasm32")]
impl Instant {
    pub(crate) fn now() -> Self { Self }
    pub(crate) fn elapsed(&self) -> std::time::Duration { std::time::Duration::ZERO }
}
