import com.storyteller_f.mermaid_kmp.renderMermaid
import com.storyteller_f.mermaid_kmp.renderMermaidClassicTheme
import com.storyteller_f.mermaid_kmp.renderMermaidWithSpacing
import kotlin.test.Test
import kotlin.test.assertContains

class MermaidDesktopTest {
    private val diagram = "flowchart LR\n A[Start] --> B[Finish]"

    @Test fun renderDefault() = assertSvg(renderMermaid(diagram))
    @Test fun renderSpacing() = assertSvg(renderMermaidWithSpacing(diagram, 80f, 100f))
    @Test fun renderClassicTheme() = assertSvg(renderMermaidClassicTheme(diagram))

    private fun assertSvg(svg: String) {
        assertContains(svg, "<svg")
        assertContains(svg, "Start")
        assertContains(svg, "Finish")
    }
}
