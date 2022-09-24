package gh.marad.chi.core.parser

import gh.marad.chi.core.parser.ChiSource.Section
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class SectionHelperKtTest {
    @Test
    fun `merging sections`() {
        val source = ChiSource("ab")
        val aSection = Section(source, 0, 0)
        val bSection = Section(source, 1, 1)

        mergeSections(aSection, bSection).getCode() shouldBe "ab"
        mergeSections(bSection, aSection).getCode() shouldBe "ab"
    }

    @Test
    fun `merging intersecting sections`() {
        val source = ChiSource("abc")
        val abSection = Section(source, 0, 1)
        val bSection = Section(source, 1, 2)

        mergeSections(abSection, bSection).getCode() shouldBe "abc"
        mergeSections(bSection, abSection).getCode() shouldBe "abc"
    }
}