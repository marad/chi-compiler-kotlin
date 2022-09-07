package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.DefineVariantType
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.VariantTypeConstructor
import gh.marad.chi.core.VariantTypeField
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.parser.readers.*

fun convertTypeDefinition(ctx: ConversionContext, definition: ParseVariantTypeDefinition): DefineVariantType {
    val typeParameterNames = definition.typeParameters.map { it.name }.toSet()
    return DefineVariantType(
        baseVariantType = ctx.resolveType(TypeNameRef(definition.typeName, null)) as VariantType,
        constructors = definition.variantConstructors.map {
            VariantTypeConstructor(
                name = it.name,
                fields = it.formalArguments.map { argument ->
                    VariantTypeField(
                        name = argument.name,
                        type = ctx.resolveType(argument.typeRef, typeParameterNames),
                        sourceSection = argument.section
                    )
                },
                sourceSection = it.section
            )
        },
        sourceSection = definition.section,
    )
}

fun getVariantConstructorTypeRef(
    typeDefinition: ParseVariantTypeDefinition,
    constructor: ParseVariantTypeDefinition.Constructor,
): TypeRef {
    val variantNameRef = VariantNameRef(
        variantType = TypeNameRef(typeDefinition.typeName, null),
        variantName = constructor.name,
        variantFields = constructor.formalArguments,
        section = null
    )

    return if (constructor.formalArguments.isEmpty()) {
        variantNameRef
    } else {
        FunctionTypeRef(
            typeParameters = typeDefinition.typeParameters,
            argumentTypeRefs = constructor.formalArguments.map { it.typeRef },
            returnType = variantNameRef,
            section = null
        )
    }
}