package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.ParseEffectDefinition
import gh.marad.chi.core.parser.readers.ParseHandle


fun convertEffectDefinition(ctx: ConversionContext, ast: ParseEffectDefinition): Expression =
    EffectDefinition(
        moduleName = ctx.currentModule,
        packageName = ctx.currentPackage,
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

fun convertHandle(ctx: ConversionContext, ast: ParseHandle): Expression {
    val body = convertBlock(ctx, ast.body)
    return Handle(
        body = body,
        cases = ast.cases.map {
            val caseScope = ctx.virtualSubscope()
            val effectLookupResult = ctx.lookup(it.effectName)
            val symbolInfo =
                ctx.namespace.getOrCreatePackage(
                    effectLookupResult.moduleName,
                    effectLookupResult.packageName
                ).scope.getSymbol(effectLookupResult.name)
                    ?: TODO("Effect ${it.effectName} not found!")
            val effectType = symbolInfo.type as FnType
            caseScope.addSymbol("resume", Type.fn(body.type, effectType.returnType), SymbolType.Local)
            it.argumentNames.zip(effectType.paramTypes).forEach { (name, type) ->
                caseScope.addSymbol(name, type, SymbolType.Argument)
            }
            ctx.withScope(caseScope) {
                HandleCase(
                    moduleName = effectLookupResult.moduleName,
                    packageName = effectLookupResult.packageName,
                    effectName = it.effectName,
                    argumentNames = it.argumentNames,
                    body = convert(ctx, it.body),
                    scope = caseScope,
                    sourceSection = it.section
                )
            }
        },
        sourceSection = ast.section
    )
}