package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.parser.readers.ParseEffectDefinition
import gh.marad.chi.core.parser.readers.ParseHandle


fun convertEffectDefinition(ctx: ConversionContext, ast: ParseEffectDefinition): Expression =
    EffectDefinition(
        name = ast.name,
        genericTypeParameters = ast.typeParameters.map { GenericTypeParameter(it.name) },
        parameters = ast.formalArguments.map {
            FnParam(
                it.name,
                ctx.resolveType(it.typeRef),
                it.section
            )
        },
        returnType = ast.returnTypeRef.let { ctx.resolveType(it) },
        sourceSection = ast.section
    )

fun convertHandle(ctx: ConversionContext, ast: ParseHandle): Expression =
    Handle(
        body = convertBlock(ctx, ast.body),
        cases = ast.cases.map {
            HandleCase(
                effectName = it.effectName,
                argumentNames = it.argumentNames,
                body = convert(ctx, it.body),
                sourceSection = it.section
            )
        },
        sourceSection = ast.section
    )
