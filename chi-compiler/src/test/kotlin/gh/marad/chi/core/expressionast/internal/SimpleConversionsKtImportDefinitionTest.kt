package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.expressionast.ConversionContext
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SimpleConversionsKtImportDefinitionTest {

    @Test
    fun `should convert module and package name`() {
        // given
        val import = sampleImport.copy(
            moduleName = testModuleName,
            packageName = testPackageName
        )

        // when
        val result = convertImportDefinition(defaultContext(), import)

        // then
        result.moduleName shouldBe testModuleName.name
        result.packageName shouldBe testPackageName.name
    }

    @Test
    fun `package alias is optional`() {
        convertImportDefinition(defaultContext(), sampleImport.copy(packageAlias = null))
            .packageAlias.shouldBeNull()
    }

    @Test
    fun `should read package alias`() {
        // given
        val import = sampleImport.copy(
            packageAlias = Alias("myAlias", testSection)
        )

        // when
        val alias = convertImportDefinition(defaultContext(), import).packageAlias

        // then
        alias.shouldNotBeNull() shouldBe "myAlias"
    }

    @Test
    fun `determine if import is from the same module`() {
        // given
        val ctx = defaultContext().inPackage(
            moduleName = testModuleName.name,
            packageName = testPackageName.name
        )
        val import = sampleImport.copy(
            moduleName = testModuleName,
            packageName = testPackageName
        )

        // then
        convertImportDefinition(ctx, import).withinSameModule.shouldBeTrue()
    }

    @Test
    fun `determine if import is from different module`() {
        // given
        val ctx = defaultContext().inPackage(
            moduleName = "other.module",
            packageName = testPackageName.name
        )
        val import = sampleImport.copy(
            moduleName = testModuleName,
            packageName = testPackageName
        )

        // then
        convertImportDefinition(ctx, import).withinSameModule.shouldBeFalse()
    }

    @Test
    fun `should convert entries`() {
        // given
        val import = sampleImport.copy(
            entries = listOf(
                importEntry(
                    name = "name",
                    alias = Alias("alias", sectionA),
                    section = sectionB
                )
            )
        )

        // when
        val entry = convertImportDefinition(defaultContext(), import).entries.first()

        // then
        entry.isTypeImport.shouldBeFalse()
        entry.isPublic.shouldBeNull()
        entry.name shouldBe "name"
        entry.alias.shouldNotBeNull() shouldBe "alias"
        entry.sourceSection shouldBe sectionB
    }

    @Test
    fun `should determine if symbol is public`() {
        // given
        val ctx = defaultContext().withPublicVariable(testModuleName, testPackageName, "variable")
        val import = sampleImport.copy(
            moduleName = testModuleName,
            packageName = testPackageName,
            entries = listOf(importEntry("variable"))
        )

        // when
        val entry = convertImportDefinition(ctx, import).entries.first()

        // then
        entry.isPublic.shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `should determine if imported symbol is a type`() {
        val ctx = defaultContext().withTypeDefinition(testModuleName, testPackageName, "SomeType")
        val import = sampleImport.copy(
            moduleName = testModuleName,
            packageName = testPackageName,
            entries = listOf(importEntry("SomeType"))
        )

        // when
        val entry = convertImportDefinition(ctx, import).entries.first()

        // then
        entry.isTypeImport.shouldBeTrue()
    }

    private fun defaultContext() = ConversionContext(GlobalCompilationNamespace())

    private fun ConversionContext.inPackage(moduleName: String, packageName: String): ConversionContext =
        also {
            it.changeCurrentPackage(moduleName, packageName)
        }

    private fun ConversionContext.withPublicVariable(
        moduleName: ModuleName,
        packageName: PackageName,
        variableName: String
    ) = also {
        it.namespace.getOrCreatePackage(moduleName.name, packageName.name)
            .scope.addSymbol(variableName, Type.intType, SymbolType.Local, public = true, mutable = false)
    }

    private fun ConversionContext.withTypeDefinition(
        moduleName: ModuleName,
        packageName: PackageName,
        typeName: String
    ) = also {
        it.namespace.getOrCreatePackage(moduleName.name, packageName.name)
            .typeRegistry.defineTypes(
                moduleName = moduleName.name,
                packageName = packageName.name,
                typeDefs = listOf(
                    ParseVariantTypeDefinition(
                        typeName = typeName,
                        typeParameters = emptyList(),
                        variantConstructors = listOf(
                            ParseVariantTypeDefinition.Constructor(
                                public = true,
                                name = typeName,
                                formalFields = emptyList(),
                                section = sectionB
                            )
                        ),
                        section = sectionA
                    )
                ),
                resolveTypeRef = { ref, typeParams -> Type.undefined }
            )
    }

    private val testModuleName = ModuleName("my.mod", sectionA)
    private val testPackageName = PackageName("my.pkg", sectionB)
    private val sampleImport =
        ParseImportDefinition(
            testModuleName,
            testPackageName,
            packageAlias = null,
            entries = emptyList(),
            testSection
        )

    private fun importEntry(name: String, alias: Alias? = null, section: ChiSource.Section? = null) =
        ParseImportDefinition.Entry(name, alias, section)
}