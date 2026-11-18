import com.storyteller_f.mermaid_kmp.FfiException
import com.storyteller_f.mermaid_kmp.renderMermaid
import com.storyteller_f.mermaid_kmp.renderMermaidClassicTheme
import com.storyteller_f.mermaid_kmp.renderMermaidWithSpacing
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class MermaidDesktopTest {
    private val diagram = "flowchart LR\n A[Start] --> B[Finish]"
    private val invalidDiagram = "sequenceDiagram\nparticipant Alice\nparticipant Bob\nAlice->>John: Hello"

    @Test fun renderDefault() = assertSvg(renderMermaid(diagram))
    @Test fun renderSpacing() = assertSvg(renderMermaidWithSpacing(diagram, 80f, 100f))
    @Test fun renderClassicTheme() = assertSvg(renderMermaidClassicTheme(diagram))

    @Test fun renderDefaultRecoversAfterError() = assertRecoversAfterError(::renderMermaid)

    @Test fun renderSpacingRecoversAfterError() =
        assertRecoversAfterError { renderMermaidWithSpacing(it, 80f, 100f) }

    @Test fun renderClassicThemeRecoversAfterError() =
        assertRecoversAfterError(::renderMermaidClassicTheme)

    private fun assertRecoversAfterError(render: (String) -> String) {
        val error = assertFailsWith<FfiException> { render(invalidDiagram) }
        assertContains(error.message.orEmpty(), "unknown participant 'John'")
        assertSvg(render(diagram))
    }

    private fun assertSvg(svg: String) {
        assertContains(svg, "<svg")
        assertContains(svg, "Start")
        assertContains(svg, "Finish")
    }
}
