package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.expressionast.ConversionContext
import gh.marad.chi.core.expressionast.generateExpressionAst
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.ParseEffectDefinition
import gh.marad.chi.core.parser.readers.ParseHandle


fun convertEffectDefinition(ctx: ConversionContext, ast: ParseEffectDefinition): Expression {
    val typeParameters = ast.typeParameters.map { GenericTypeParameter(it.name) }
    val typeParameterNames = typeParameters.map { it.name }.toSet()
    return EffectDefinition(
        moduleName = ctx.currentModule,
        packageName = ctx.currentPackage,
        name = ast.name,
        genericTypeParameters = typeParameters,
        parameters = ast.formalArguments.map {
            FnParam(
                it.name,
                ctx.resolveType(it.typeRef, typeParameterNames),
                it.section
            )
        },
        returnType = ast.returnTypeRef.let { ctx.resolveType(it, typeParameterNames) },
        sourceSection = ast.section
    )
}

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
            caseScope.addSymbol("resume", Type.fn(body.type, effectType.returnType), SymbolType.Local, public = false)
            it.argumentNames.zip(effectType.paramTypes).forEach { (name, type) ->
                caseScope.addSymbol(name, type, SymbolType.Argument, public = false)
            }
            ctx.withScope(caseScope) {
                HandleCase(
                    moduleName = effectLookupResult.moduleName,
                    packageName = effectLookupResult.packageName,
                    effectName = it.effectName,
                    argumentNames = it.argumentNames,
                    body = generateExpressionAst(ctx, it.body),
                    scope = caseScope,
                    sourceSection = it.section
                )
            }
        },
        sourceSection = ast.section
    )
}