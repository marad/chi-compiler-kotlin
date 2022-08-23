package gh.marad.chi.core.types

import gh.marad.chi.ast
import gh.marad.chi.core.AnyType
import gh.marad.chi.core.NameDeclaration
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.analyze
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class AnyTypeSpec : FunSpec({
    test("should read 'any' type") {
        ast(
            """
                val x: any = 1
            """.trimIndent(), ignoreCompilationErrors = true
        ).shouldBeTypeOf<NameDeclaration>() should {
            it.expectedType
                .shouldNotBeNull()
                .shouldBeTypeOf<AnyType>()
        }
    }

    test("'any' type should be matched by any other type") {
        val msgs = analyze(ast("val x: any = 1", ignoreCompilationErrors = true))
        msgs.shouldBeEmpty()
    }

    test("foo") {
        ast(
            """
                data Foo = Foo(i: int)
                fn f(param: any): Foo { param as Foo }
                val foo = f(Foo(10))
            """.trimIndent()
        ).shouldBeTypeOf<NameDeclaration>() should {
            it.type.shouldBeTypeOf<VariantType>() should {
                it.name shouldBe "user/default.Foo"
            }
        }
    }

})