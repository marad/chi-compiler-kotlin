package gh.marad.chi.core

import gh.marad.chi.core.compiled.Compiled
import gh.marad.chi.core.compiled.LongValue
import gh.marad.chi.core.compiled.ReadLocalVariable
import gh.marad.chi.core.compiled.ReadPackageVariable
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

fun Compiled.shouldBeLong(value: Long) =
    shouldBeTypeOf<LongValue>().value shouldBe value

fun Compiled.shouldBeFunction(matcher: (gh.marad.chi.core.compiled.Function) -> Unit) =
    shouldBeTypeOf<gh.marad.chi.core.compiled.Function>().should(matcher)

fun Compiled.shouldBeLocalVariableRead(name: String) =
    shouldBeTypeOf<ReadLocalVariable>().name shouldBe name

fun Compiled.shouldBePackageVariableRead(name: String) =
    shouldBeTypeOf<ReadPackageVariable>().name shouldBe name

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