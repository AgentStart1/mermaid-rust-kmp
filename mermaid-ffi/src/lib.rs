use boltffi::*;

boltffi::scaffolding!();

#[export]
pub fn render_mermaid(input: String) -> Result<String, String> {
    mermaid_rs_renderer::render(&input).map_err(|err| err.to_string())
}

#[export]
pub fn render_mermaid_with_spacing(
    input: String,
    node_spacing: f32,
    rank_spacing: f32,
) -> Result<String, String> {
    let opts = mermaid_rs_renderer::RenderOptions::default()
        .with_node_spacing(node_spacing)
        .with_rank_spacing(rank_spacing);
    mermaid_rs_renderer::render_with_options(&input, opts).map_err(|err| err.to_string())
}

#[export]
pub fn render_mermaid_classic_theme(input: String) -> Result<String, String> {
    mermaid_rs_renderer::render_with_options(
        &input,
        mermaid_rs_renderer::RenderOptions::mermaid_default(),
    )
    .map_err(|err| err.to_string())
}
