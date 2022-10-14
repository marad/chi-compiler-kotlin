package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.expressionast.ConversionContext
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

val testSource = ChiSource("dummy code")
val testSection = testSource.getSection(0, 5)
val sectionA = testSource.getSection(0, 1)
val sectionB = testSource.getSection(1, 2)
val sectionC = testSource.getSection(2, 3)

val defaultModule = ModuleName(CompilationDefaults.defaultModule, null)
val otherModule = ModuleName("other.module", null)
val defaultPackage = PackageName(CompilationDefaults.defaultPacakge, null)
val otherPackage = PackageName("other.pkg", null)

val intTypeRef = TypeNameRef("int", sectionA)
val stringTypeRef = TypeNameRef("string", sectionA)

val sampleParseBlock = ParseBlock(
    body = emptyList(),
    testSection
)

fun arg(name: String, typeName: String) = FormalArgument(name, TypeNameRef(typeName, sectionB), sectionA)
fun intArg(name: String) = FormalArgument(name, intTypeRef, sectionA)
fun stringArg(name: String) = FormalArgument(name, stringTypeRef, sectionA)

fun Expression.shouldBeVariable(name: String, section: ChiSource.Section? = null) {
    this.shouldBeTypeOf<VariableAccess>() should {
        it.name shouldBe name
        if (section != null) {
            it.sourceSection shouldBe section
        }
    }
}

fun defaultContext() = ConversionContext(GlobalCompilationNamespace())


fun ConversionContext.inPackage(moduleName: String, packageName: String): ConversionContext =
    also {
        it.changeCurrentPackage(moduleName, packageName)
    }

fun ConversionContext.addPackageAlias(
    moduleName: ModuleName,
    packageName: PackageName,
    packageAlias: String
) {
    this.imports.addImport(
        Import(
            moduleName.name,
            packageName.name,
            packageAlias = packageAlias,
            entries = emptyList(),
            withinSameModule = this.currentModule == moduleName.name,
            null
        )
    )
}

fun ConversionContext.importSymbol(
    moduleName: ModuleName,
    packageName: PackageName,
    symbolName: String,
    packageAlias: String? = null,
    alias: String? = null
) {
    val isType = this.namespace.getOrCreatePackage(moduleName.name, packageName.name)
        .typeRegistry.getTypeOrNull(symbolName) != null

    this.imports.addImport(
        Import(
            moduleName.name,
            packageName.name,
            packageAlias = packageAlias,
            withinSameModule = this.currentModule == moduleName.name,
            entries = listOf(
                ImportEntry(
                    name = symbolName,
                    alias = alias,
                    isTypeImport = isType,
                    isPublic = true,
                    sourceSection = null
                )
            ),
            sourceSection = null
        )
    )
}

fun ConversionContext.addPublicSymbol(
    moduleName: ModuleName,
    packageName: PackageName,
    variableName: String,
    type: Type = Type.intType
) = also {
    it.namespace.getOrCreatePackage(moduleName.name, packageName.name)
        .scope.addSymbol(variableName, type, SymbolType.Local, public = true, mutable = false)
}

fun ConversionContext.addPublicSymbol(
    variableName: String,
    type: Type = Type.intType
) = this.addPublicSymbol(
    moduleName = ModuleName(currentModule, null),
    packageName = PackageName(currentPackage, null),
    variableName, type
)

fun ConversionContext.addTypeDefinition(
    typeName: String,
    constructorNames: List<String>? = null
) = this.addTypeDefinition(
    moduleName = ModuleName(currentModule, null),
    packageName = PackageName(currentPackage, null),
    typeName, constructorNames
)


fun ConversionContext.addTypeDefinition(
    moduleName: ModuleName,
    packageName: PackageName,
    typeName: String,
    constructorNames: List<String>? = null
): Type = let {
    val constructors = (constructorNames ?: listOf(typeName)).map {
        ParseVariantTypeDefinition.Constructor(
            public = true,
            name = it,
            formalFields = emptyList(),
            section = null
        )
    }
    val typeRegistry = it.namespace
        .getOrCreatePackage(moduleName.name, packageName.name)
        .typeRegistry
    typeRegistry.defineTypes(
        moduleName = moduleName.name,
        packageName = packageName.name,
        typeDefs = listOf(
            ParseVariantTypeDefinition(
                typeName = typeName,
                typeParameters = emptyList(),
                variantConstructors = constructors,
                section = sectionA
            )
        ),
        resolveTypeRef = this::resolveType
    )
    typeRegistry.getTypeOrNull(typeName)!!
}

