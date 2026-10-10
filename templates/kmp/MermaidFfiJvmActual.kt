// Public KMP facade backed by generated Kotlin/JNI bindings.

package com.storyteller_f.mermaid_kmp

private fun com.storyteller_f.mermaid_kmp.jvm.FfiException.toBoltFfiCommon(): FfiException = FfiException(-1, message ?: "")

actual fun renderMermaid(input: String): String {
    try {
        return com.storyteller_f.mermaid_kmp.jvm.renderMermaid(input)
    } catch (err: com.storyteller_f.mermaid_kmp.jvm.FfiException) {
        throw err.toBoltFfiCommon()
    }
}

actual fun renderMermaidWithSpacing(input: String, nodeSpacing: Float, rankSpacing: Float): String {
    try {
        return com.storyteller_f.mermaid_kmp.jvm.renderMermaidWithSpacing(input, nodeSpacing, rankSpacing)
    } catch (err: com.storyteller_f.mermaid_kmp.jvm.FfiException) {
        throw err.toBoltFfiCommon()
    }
}

actual fun renderMermaidClassicTheme(input: String): String {
    try {
        return com.storyteller_f.mermaid_kmp.jvm.renderMermaidClassicTheme(input)
    } catch (err: com.storyteller_f.mermaid_kmp.jvm.FfiException) {
        throw err.toBoltFfiCommon()
    }
}
