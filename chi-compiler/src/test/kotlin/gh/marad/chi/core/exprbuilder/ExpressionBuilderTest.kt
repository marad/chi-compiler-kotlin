package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.Atom
import gh.marad.chi.core.parser2.BoolValue
import gh.marad.chi.core.parser2.FloatValue
import gh.marad.chi.core.parser2.LongValue
import gh.marad.chi.core.parser2.StringValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class ExpressionBuilderTest : FunSpec({

    test("convert atom values") {
        parseAstToAtom(BoolValue(true)).shouldBeBooleanAtom(true)
        parseAstToAtom(BoolValue(false)).shouldBeBooleanAtom(false)
        parseAstToAtom(LongValue(10)).shouldBeLongAtom(10)
        parseAstToAtom(FloatValue(10.5f)).shouldBeFloatAtom(10.5f)
        parseAstToAtom(StringValue("hello")).shouldBeStringAtom("hello")
    }
})


fun Any.shouldBeBooleanAtom(value: Boolean) =
    this.shouldBeTypeOf<Atom>().value shouldBe if (value) "true" else "false"

fun Any.shouldBeLongAtom(value: Long) =
    this.shouldBeTypeOf<Atom>().value shouldBe value.toString()

fun Any.shouldBeFloatAtom(value: Float) =
    this.shouldBeTypeOf<Atom>().value shouldBe value.toString()

fun Any.shouldBeStringAtom(value: String) =
    this.shouldBeTypeOf<Atom>().value shouldBe value
