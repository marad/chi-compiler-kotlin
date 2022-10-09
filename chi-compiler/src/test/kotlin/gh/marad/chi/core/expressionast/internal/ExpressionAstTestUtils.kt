package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.expressionast.ConversionContext
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.ChiSource
import gh.marad.chi.core.parser.readers.ModuleName
import gh.marad.chi.core.parser.readers.PackageName
import gh.marad.chi.core.parser.readers.ParseVariantTypeDefinition

val testSource = ChiSource("dummy code")
val testSection = testSource.getSection(0, 5)
val sectionA = testSource.getSection(0, 1)
val sectionB = testSource.getSection(1, 2)
val sectionC = testSource.getSection(2, 3)

fun defaultContext() = ConversionContext(GlobalCompilationNamespace())

fun ConversionContext.inPackage(moduleName: String, packageName: String): ConversionContext =
    also {
        it.changeCurrentPackage(moduleName, packageName)
    }

fun ConversionContext.withPublicVariable(
    moduleName: ModuleName,
    packageName: PackageName,
    variableName: String,
    type: Type = Type.intType
) = also {
    it.namespace.getOrCreatePackage(moduleName.name, packageName.name)
        .scope.addSymbol(variableName, type, SymbolType.Local, public = true, mutable = false)
}

fun ConversionContext.withPublicVariable(
    variableName: String,
    type: Type = Type.intType
) = this.withPublicVariable(
    moduleName = ModuleName(currentModule, null),
    packageName = PackageName(currentPackage, null),
    variableName, type
)

fun ConversionContext.withTypeDefinition(
    typeName: String,
    constructorNames: List<String>? = null
) = this.withTypeDefinition(
    moduleName = ModuleName(currentModule, null),
    packageName = PackageName(currentPackage, null),
    typeName, constructorNames
)


fun ConversionContext.withTypeDefinition(
    moduleName: ModuleName,
    packageName: PackageName,
    typeName: String,
    constructorNames: List<String>? = null
) = also {
    val constructors = (constructorNames ?: listOf(typeName)).map {
        ParseVariantTypeDefinition.Constructor(
            public = true,
            name = it,
            formalFields = emptyList(),
            section = null
        )
    }
    it.namespace.getOrCreatePackage(moduleName.name, packageName.name)
        .typeRegistry.defineTypes(
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
            resolveTypeRef = { ref, typeParams -> Type.undefined }
        )
}

