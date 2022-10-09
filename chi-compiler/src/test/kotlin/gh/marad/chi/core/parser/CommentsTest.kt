package gh.marad.chi.core.parser

import org.junit.jupiter.api.Test

class CommentsTest {
    @Test
    fun `should skip single line comments`() {
        testParse(
            """
                // this is a comment
                5
            """.trimIndent()
        )[0].shouldBeLongValue(5)
    }

    @Test
    fun `should skip multiline comments`() {
        testParse(
            """
                /* this is
                   a multiline comment */
                5   
            """.trimIndent()
        )[0].shouldBeLongValue(5)
    }
}