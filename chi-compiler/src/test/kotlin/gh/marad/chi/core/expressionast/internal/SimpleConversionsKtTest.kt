package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class SimpleConversionsKtTest {
    @Test
    fun `generating simple atoms`() {
        convertAtom(LongValue(10, testSection)).shouldBeAtom("10", Type.intType, testSection)
        convertAtom(FloatValue(0.5f, testSection)).shouldBeAtom("0.5", Type.floatType, testSection)
        convertAtom(BoolValue(true, testSection)).shouldBeAtom("true", Type.bool, testSection)
        convertAtom(BoolValue(false, testSection)).shouldBeAtom("false", Type.bool, testSection)
        convertAtom(StringValue("test", testSection)).shouldBeAtom("test", Type.string, testSection)
    }

    @Test
    fun `generating interpolated string`() {
        // when
        val result = convertInterpolatedString(
            defaultContext(),
            ParseInterpolatedString(
                section = testSection,
                parts = listOf(
                    StringText("test", sectionA),
                    ParseInterpolation(LongValue(10), sectionB)
                )
            )
        )

        // then
        result.shouldBeTypeOf<InterpolatedString>() should {
            it.parts shouldHaveSize 2
            it.parts[0].shouldBeAtom("test", Type.string)
            it.parts[1].shouldBeTypeOf<Cast>().should {
                it.expression.shouldBeAtom("10", Type.intType)
                it.targetType shouldBe Type.string
            }
        }
    }

    @Test
    fun `string text in interpolation should be converted to simple string atom`() {
        convertStringText(StringText("test", testSection))
            .shouldBeAtom("test", Type.string, testSection)
    }

    @Test
    fun `code interpolations should be converted and cast to string`() {
        convertInterpolation(defaultContext(), ParseInterpolation(LongValue(10), testSection))
            .shouldBeTypeOf<Cast>() should {
            it.targetType shouldBe Type.string
            it.expression.shouldBeAtom("10", Type.intType)
        }
    }

    @Test
    fun `converting null package definition should produce null value`() {
        convertPackageDefinition(null).shouldBeNull()
    }

    @Test
    fun `package definition conversion`() {
        val result = convertPackageDefinition(
            ParsePackageDefinition(
                ModuleName("my.mod", sectionA),
                PackageName("my.pkg", sectionB),
                sectionC
            )
        )

        result.shouldNotBeNull().shouldBeTypeOf<Package>() should {
            it.moduleName shouldBe "my.mod"
            it.packageName shouldBe "my.pkg"
            it.sourceSection shouldBe sectionC
        }
    }

    @Test
    fun `block conversion`() {
        convertBlock(defaultContext(), ParseBlock(listOf(LongValue(10)), testSection)) should {
            it.body shouldHaveSize 1
            it.body[0].shouldBeAtom("10", Type.intType)
            it.sourceSection shouldBe testSection
        }
    }

    @Test
    fun `convert binary operator`() {
        // when
        val result = convertBinaryOp(
            defaultContext(),
            ParseBinaryOp(
                op = "generic operation",
                left = StringValue("hello"),
                right = LongValue(20),
                section = testSection
            )
        )

        // then
        result.op shouldBe "generic operation"
        result.left.shouldBeAtom("hello", Type.string)
        result.right.shouldBeAtom("20", Type.intType)
        result.sourceSection shouldBe testSection
    }

    @Test
    fun convertCast() {
    }

    @Test
    fun convertIs() {
    }

    @Test
    fun convertNot() {
    }
}