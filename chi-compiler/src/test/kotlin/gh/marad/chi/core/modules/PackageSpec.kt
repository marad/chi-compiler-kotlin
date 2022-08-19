package gh.marad.chi.core.modules

import gh.marad.chi.ast
import gh.marad.chi.compile
import gh.marad.chi.core.GlobalCompilationNamespace
import gh.marad.chi.core.InvalidModuleName
import gh.marad.chi.core.InvalidPackageName
import gh.marad.chi.core.analyze
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class PackageSpec : FunSpec({
    test("should set current module and package and define name there") {
        // when
        val namespace = GlobalCompilationNamespace()
        val expressions = compile(
            """
            package my.module/some.system
            val millis = fn() {}
        """.trimIndent(), namespace
        )

        // then
        expressions shouldHaveSize 2
        expressions.first()
            .shouldBeTypeOf<gh.marad.chi.core.Package>()
            .should { pkg ->
                pkg.moduleName shouldBe "my.module"
                pkg.packageName shouldBe "some.system"
            }

        // and
        val targetScope = namespace.getOrCreatePackage("my.module", "some.system").scope
        targetScope.containsSymbol("millis") shouldBe true

        val defaultScope = namespace.getDefaultPackage().scope
        defaultScope.containsSymbol("millis") shouldBe false
    }

    test("should not allow empty module name") {
        // given
        val packageDefinition = ast(
            """
            package /some.system
        """.trimIndent(), ignoreCompilationErrors = true
        )

        // when
        val messages = analyze(packageDefinition)

        // then
        messages shouldHaveSize 1
        messages[0].shouldBeTypeOf<InvalidModuleName>()
            .should { it.moduleName shouldBe "" }
    }

    test("should not allow empty package name") {
        // given
        val packageDefinition = ast(
            """
            package some.module/
        """.trimIndent(), ignoreCompilationErrors = true
        )

        // when
        val messages = analyze(packageDefinition)

        // then
        messages shouldHaveSize 1
        messages[0].shouldBeTypeOf<InvalidPackageName>()
            .should { it.packageName shouldBe "" }
    }
})