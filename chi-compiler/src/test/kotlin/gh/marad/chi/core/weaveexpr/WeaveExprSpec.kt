package gh.marad.chi.core.weaveexpr

import gh.marad.chi.core.parser.readers.ParseFnCall
import gh.marad.chi.core.parser.readers.ParseWeave
import gh.marad.chi.core.parser.readers.ParseWeavePlaceholder
import gh.marad.chi.core.shouldBeStringValue
import gh.marad.chi.core.testParse
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class WeaveExprSpec {
    @Test
    fun `simple parsing weave with placeholder`() {
        val code = """
            "hello" ~> str.toUpper(_)
        """.trimIndent()
        val ast = testParse(code)

        ast[0].shouldBeTypeOf<ParseWeave>() should { weave ->
            weave.value.shouldBeStringValue("hello")
            weave.opTemplate.shouldBeTypeOf<ParseFnCall>() should { call ->
                call.arguments.first().shouldBeTypeOf<ParseWeavePlaceholder>()
            }
        }
    }

    @Test
    fun `parsing weave chain`() {
        val code = """
            "2hello" 
                ~> str.toUpper(_)
                ~> _[0]
                ~> 2 + _
        """.trimIndent()
        TODO()
    }
}