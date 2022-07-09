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

fun Expression.shouldBeAtom(value: String, type: Type, location: Location): Atom =
    shouldBeAtom(value, type).also {
        should {
            it.location shouldBe location
        }
    }

fun Expression.shouldBeVariableAccess(name: String, location: Location): VariableAccess =
    shouldBeTypeOf<VariableAccess>().also {
        should {
            it.name shouldBe name
            it.location shouldBe location
        }
    }


fun Expression.shouldBeFn(matcher: (Fn) -> Unit) =
    shouldBeTypeOf<Fn>().should(matcher)