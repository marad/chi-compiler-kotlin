package gh.marad.chi.core.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChiSourceTest : FunSpec({

    val sampleCode = """
        this is 
        some text
        in multple lines
    """.trimIndent()

    val source = ChiSource(sampleCode)

    test("calculating start line and column number") {
        val start = sampleCode.indexOf("text")
        val section = source.getSection(start, start + 3)
        section.startLine shouldBe 2
        section.startColumn shouldBe 5
        sampleCode.lines()[section.startLine - 1].substring(
            section.startColumn,
            section.startColumn + 4
        ) shouldBe "text"
        section.getCode() shouldBe "text"
    }


    test("calculating end line and column number") {
        val start = sampleCode.indexOf("text")
        val section = source.getSection(start, start + 3)
        section.endLine shouldBe 2
        section.endColumn shouldBe 9
        sampleCode.lines()[section.endLine - 1].substring(
            section.endColumn - 4,
            section.endColumn
        ) shouldBe "text"
        section.getCode() shouldBe "text"
    }
})
