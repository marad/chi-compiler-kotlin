package gh.marad.chi.core

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

fun Expression.shouldBeAtom(value: String, type: Type): Atom =
    shouldBeTypeOf<Atom>().also {
        should {
            it.value shouldBe value
            it.type shouldBe type
        }
    }

fun Expression.shouldBeVariableAccess(name: String): VariableAccess =
    shouldBeTypeOf<VariableAccess>().also {
        should {
            it.name shouldBe name
        }
    }


fun Expression.shouldBeFn(matcher: (Fn) -> Unit) =
    shouldBeTypeOf<Fn>().should(matcher)

fun FnParam.shouldBeFnParam(name: String, type: Type) =
    shouldBeTypeOf<FnParam>().also {
        should {
            it.name shouldBe name
            it.type shouldBe type
        }
    }

fun Expression.shouldBeEmptyBlock() =
    shouldBeTypeOf<Block>().also {
        it.body shouldBe emptyList()
    }

fun Expression.shouldBeBlock(matcher: (Block) -> Unit) =
    shouldBeTypeOf<Block>().should(matcher)