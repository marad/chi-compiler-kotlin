package gh.marad.chi.core.parser

import ChiParser
import ChiParser.VariantTypeDefinitionContext
import gh.marad.chi.core.*

object VariantTypeDefinitionReader {
    fun read(context: ParsingContext, ctx: VariantTypeDefinitionContext): DefineVariantType {
        val (typeDefinition, variantConstructors) = readAndDefineVariantTypeAndConstructors(context, ctx)
        defineScopeSymbols(context, typeDefinition)
        return DefineVariantType(
            typeDefinition.baseType,
            variantConstructors,
            makeLocation(ctx)
        )
    }

    private fun readAndDefineVariantTypeAndConstructors(
        context: ParsingContext,
        ctx: VariantTypeDefinitionContext
    ): Pair<VariantTypeDefinition, List<VariantTypeConstructor>> {
        val simpleTypeName = ctx.typeName.text
        val genericTypeParameters = GenericsReader.readGenericTypeParameterDefinitions(ctx.generic_type_definitions())
        val moduleName = context.currentModule
        val packageName = context.currentPackage
        val baseVariantType =
            VariantType(moduleName, packageName, simpleTypeName, genericTypeParameters, emptyMap(), null)
        // To allow reading recurring types I first create temporary descriptor without variants.
        // This is needed so that the defined type can be properly recognized for fields.
        // It's later replaced by fully defined type descriptor
        val temporaryTypeWithoutVariants =
            VariantTypeDefinition(baseVariantType, emptyList())
        context.currentPackageDescriptor.variantTypes.defineType(temporaryTypeWithoutVariants)

        val variantConstructors = ctx.variantTypeConstructors()?.variantTypeConstructor()?.map {
            readVariantTypeConstructor(context, it)
        } ?: emptyList()

        val variants = variantConstructors.map { it.toVariant() }
        val baseType = VariantTypeDefinition(baseVariantType.copy(variant = variants.singleOrNull()), variants)
        context.currentPackageDescriptor.variantTypes.defineType(baseType)
        return Pair(baseType, variantConstructors)
    }

    private fun defineScopeSymbols(
        context: ParsingContext,
        typeDefinition: VariantTypeDefinition
    ) {
        typeDefinition.variants.forEach { constructor ->
            val variant = VariantType.Variant(constructor.variantName, constructor.fields)
            val type = typeDefinition.construct(variant)
            if (constructor.fields.isNotEmpty()) {
                context.currentPackageDescriptor.scope.addSymbol(
                    name = constructor.variantName,
                    type = if (typeDefinition.baseType.genericTypeParameters.isEmpty()) {
                        Type.fn(type, *constructor.fields.map { it.type }.toTypedArray())
                    } else {
                        Type.genericFn(
                            typeDefinition.baseType.genericTypeParameters,
                            type,
                            *constructor.fields.map { it.type }.toTypedArray()
                        )
                    },
                    scope = SymbolScope.Package,
                    false
                )
            } else {
                context.currentPackageDescriptor.scope.addSymbol(
                    name = constructor.variantName,
                    type = type,
                    scope = SymbolScope.Package,
                )
            }
        }
    }

    private fun readVariantTypeConstructor(
        context: ParsingContext,
        ctx: ChiParser.VariantTypeConstructorContext
    ): VariantTypeConstructor {
        val constructorName = ctx.variantName.text
        val fields = ctx.func_argument_definitions()?.argumentsWithTypes()?.argumentWithType()?.map {
            val name = it.ID().text
            val type = TypeReader.read(context, it.type())
            VariantTypeField(name, type, makeLocation(it))
        } ?: emptyList()
        return VariantTypeConstructor(constructorName, fields, makeLocation(ctx))
    }

}
