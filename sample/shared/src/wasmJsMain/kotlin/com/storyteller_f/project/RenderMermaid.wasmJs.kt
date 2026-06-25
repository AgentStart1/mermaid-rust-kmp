package com.storyteller_f.project

import com.storyteller_f.mermaid_kmp.renderMermaid

actual fun renderMermaidDiagram(input: String): String {
    return renderMermaid(input)
}
