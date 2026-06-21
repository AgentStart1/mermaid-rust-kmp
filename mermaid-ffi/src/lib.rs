use boltffi::*;

fn unwrap_render<E: std::fmt::Display>(result: Result<String, E>) -> String {
    match result {
        Ok(svg) => svg,
        Err(e) => panic!("{e}"),
    }
}

#[export]
pub fn render_mermaid(input: String) -> String {
    unwrap_render(mermaid_rs_renderer::render(&input))
}

#[export]
pub fn render_mermaid_with_spacing(input: String, node_spacing: f32, rank_spacing: f32) -> String {
    let opts = mermaid_rs_renderer::RenderOptions::default()
        .with_node_spacing(node_spacing)
        .with_rank_spacing(rank_spacing);
    unwrap_render(mermaid_rs_renderer::render_with_options(&input, opts))
}

#[export]
pub fn render_mermaid_classic_theme(input: String) -> String {
    unwrap_render(mermaid_rs_renderer::render_with_options(
        &input,
        mermaid_rs_renderer::RenderOptions::mermaid_default(),
    ))
}
