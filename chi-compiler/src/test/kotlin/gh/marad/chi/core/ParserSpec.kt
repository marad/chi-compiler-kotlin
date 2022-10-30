package gh.marad.chi.core

import gh.marad.chi.ast
import gh.marad.chi.core.Type.Companion.fn
import gh.marad.chi.core.Type.Companion.intType
import gh.marad.chi.core.namespace.CompilationScope
import gh.marad.chi.core.namespace.ScopeType
import gh.marad.chi.core.namespace.SymbolType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

@Suppress("unused")
class ParserSpec : FunSpec({

    test("should read lambda function invocation expression") {
        val scope = CompilationScope(ScopeType.Package)
        ast("({ 1 })()", scope)
            .shouldBeTypeOf<FnCall>()
            .should {
                it.function.shouldBeTypeOf<Group>().should { group ->
                    group.value.shouldBeFn { fn ->
                        fn.parameters shouldBe emptyList()
                        fn.returnType shouldBe intType
                        fn.body.shouldBeBlock { block ->
                            block.body[0].shouldBeAtom("1", intType)
                        }
                    }
                }
                it.parameters shouldBe emptyList()
            }
    }

    test("should read nested function invocations") {
        val scope = CompilationScope(ScopeType.Package)
        scope.addSymbol("a", fn(intType, intType), SymbolType.Local)
        scope.addSymbol("b", fn(intType, intType), SymbolType.Local)
        scope.addSymbol("x", intType, SymbolType.Local)
        ast("a(b(x))", scope)
            .shouldBeTypeOf<FnCall>()
            .should { aFnCall ->
                aFnCall.parameters
                    .shouldHaveSize(1)
                    .first()
                    .shouldBeTypeOf<FnCall>().should { bFnCall ->
                        bFnCall.parameters
                            .shouldHaveSize(1)
                            .first()
                            .shouldBeTypeOf<VariableAccess>()
                            .should {
                                it.name.shouldBe("x")
                            }
                    }
            }
    }
})