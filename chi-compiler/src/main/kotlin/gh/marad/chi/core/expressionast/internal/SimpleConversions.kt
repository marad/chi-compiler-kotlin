package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.expressionast.ConversionContext
import gh.marad.chi.core.expressionast.generateExpressionAst
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.*


fun convertAtom(value: BoolValue) =
    if (value.value) Atom.t(value.section)
    else Atom.f(value.section)

fun convertAtom(value: FloatValue) =
    Atom.float(value.value, value.section)

fun convertAtom(ast: LongValue) =
    Atom.int(ast.value, ast.section)

fun convertAtom(ast: StringValue) =
    Atom.string(ast.value, ast.section)

fun convertInterpolatedString(ctx: ConversionContext, ast: ParseInterpolatedString): Expression {
    val parts = ast.parts.map { generateExpressionAst(ctx, it) }
    return InterpolatedString(parts, ast.section)
}

fun convertInterpolation(ctx: ConversionContext, ast: ParseInterpolation): Expression {
    return Cast(generateExpressionAst(ctx, ast.value), Type.string, ast.section)
}

fun convertStringText(ast: StringText): Expression =
    Atom.string(ast.text, ast.section)

fun convertPackageDefinition(ast: ParsePackageDefinition?): Package? =
    ast?.let {
        Package(ast.moduleName.name, ast.packageName.name, ast.section)
    }

fun convertImportDefinition(ctx: ConversionContext, ast: ParseImportDefinition): Import {
    return Import(
        moduleName = ast.moduleName.name,
        packageName = ast.packageName.name,
        packageAlias = ast.packageAlias?.alias,
        entries = ast.entries.map {
            val isTypeImport = ctx.namespace.getOrCreatePackage(ast.moduleName.name, ast.packageName.name)
                .typeRegistry.getTypeOrNull(it.name) != null
            val targetSymbol = ctx.namespace.getOrCreatePackage(ast.moduleName.name, ast.packageName.name)
                .scope.getSymbol(it.name)
            ImportEntry(
                it.name,
                it.alias?.alias,
                isTypeImport = isTypeImport,
                isPublic = targetSymbol?.public,
                sourceSection = it.section
            )
        },
        withinSameModule = ast.moduleName.name == ctx.currentModule,
        sourceSection = ast.section
    )
}

fun convertBlock(ctx: ConversionContext, ast: ParseBlock): Block =
    Block(
        body = ast.body.map { generateExpressionAst(ctx, it) },
        sourceSection = ast.section
    )


fun convertBinaryOp(ctx: ConversionContext, ast: ParseBinaryOp): InfixOp =
    InfixOp(ast.op, generateExpressionAst(ctx, ast.left), generateExpressionAst(ctx, ast.right), ast.section)

fun convertCast(ctx: ConversionContext, ast: ParseCast): Expression =
    Cast(
        expression = generateExpressionAst(ctx, ast.value),
        targetType = ctx.resolveType(ast.typeRef),
        sourceSection = ast.section
    )

fun convertIs(ctx: ConversionContext, ast: ParseIs): Expression {
    return Is(
        value = generateExpressionAst(ctx, ast.value),
        typeOrVariant = ast.typeName,
        sourceSection = ast.section
    ).also {
        fillTypeVariantForNamedVariableInIfElse(ctx, it)
    }
}

private fun fillTypeVariantForNamedVariableInIfElse(ctx: ConversionContext, it: Is) {
    val ifCtx = ctx.currentIfReadingContext
    if (ifCtx != null) {
        val lookupResult = ctx.lookupType(it.typeOrVariant)
        val valueType = lookupResult.type
        if (it.value is VariableAccess && valueType is VariantType) {
            val symbol = ctx.currentScope.getSymbol(it.value.name)!!
            ifCtx.thenScope.addSymbol(
                name = symbol.name,
                type = valueType,
                scope = SymbolType.Overwrite,
                public = symbol.public,
                mutable = symbol.mutable
            )
        }
    }
}

fun convertNot(ctx: ConversionContext, ast: ParseNot): Expression =
    PrefixOp(
        op = "!",
        expr = generateExpressionAst(ctx, ast.value),
        sourceSection = ast.section
    )


