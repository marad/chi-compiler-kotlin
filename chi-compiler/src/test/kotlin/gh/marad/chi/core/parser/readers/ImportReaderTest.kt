package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.testParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class ImportReaderTest {
    @Test
    fun `parse import definition`() {
        val code = "import some.module/some.pkg as pkgAlias { foo as fooAlias, bar as barAlias }"
        val ast = testParse(code)
        ast shouldHaveSize 1
        ast[0].shouldBeTypeOf<ParseImportDefinition>() should {
            it.moduleName.name shouldBe "some.module"
            it.packageName.name shouldBe "some.pkg"
            it.packageAlias?.alias shouldBe "pkgAlias"
            it.entries shouldHaveSize 2
            it.entries[0] should { fooEntry ->
                fooEntry.name shouldBe "foo"
                fooEntry.alias?.alias shouldBe "fooAlias"
                fooEntry.section?.getCode() shouldBe "foo as fooAlias"
            }
            it.entries[1] should { barEntry ->
                barEntry.name shouldBe "bar"
                barEntry.alias?.alias shouldBe "barAlias"
                barEntry.section?.getCode() shouldBe "bar as barAlias"
            }
            it.section?.getCode() shouldBe code
        }
    }

}