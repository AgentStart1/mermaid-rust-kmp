use boltffi::*;
use mermaid_rs_renderer;

#[export]
pub fn render_mermaid(input: String) -> String {
    match mermaid_rs_renderer::render(&input) {
        Ok(svg) => svg,
        Err(e) => format!("Error: {e}"),
    }
}

#[export]
pub fn render_mermaid_with_spacing(input: String, node_spacing: f64, rank_spacing: f64) -> String {
    let opts = mermaid_rs_renderer::RenderOptions::default()
        .with_node_spacing(node_spacing as f32)
        .with_rank_spacing(rank_spacing as f32);
    match mermaid_rs_renderer::render_with_options(&input, opts) {
        Ok(svg) => svg,
        Err(e) => format!("Error: {e}"),
    }
}

#[export]
pub fn render_mermaid_classic_theme(input: String) -> String {
    let opts = mermaid_rs_renderer::RenderOptions::mermaid_default();
    match mermaid_rs_renderer::render_with_options(&input, opts) {
        Ok(svg) => svg,
        Err(e) => format!("Error: {e}"),
    }
}
