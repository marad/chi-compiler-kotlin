package gh.marad.chi.core.parser.readers

import gh.marad.chi.core.testParse
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class PackageReaderTest {
    @Test
    fun `parse package definition`() {
        val result = testParse("package my.module/some.pkg")
        result[0].shouldBeTypeOf<ParsePackageDefinition>().should {
            it.moduleName.name shouldBe "my.module"
            it.packageName.name shouldBe "some.pkg"
        }
    }

    @Test
    fun `can parse empty module`() {
        val result = testParse("package /some.pkg")
        result[0].shouldBeTypeOf<ParsePackageDefinition>().should {
            it.moduleName.name shouldBe ""
            it.packageName.name shouldBe "some.pkg"
        }
    }

    @Test
    fun `can parse empty package`() {
        val result = testParse("package my.module/")
        result[0].shouldBeTypeOf<ParsePackageDefinition>().should {
            it.moduleName.name shouldBe "my.module"
            it.packageName.name shouldBe ""
        }
    }
}