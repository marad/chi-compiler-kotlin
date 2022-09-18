package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
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

fun convertPackageDefinition(ast: ParsePackageDefinition?): Package? =
    ast?.let {
        Package(ast.moduleName.name, ast.packageName.name, ast.section)
    }

fun convertImportDefinition(ast: ParseImportDefinition): Import =
    Import(
        moduleName = ast.moduleName.name,
        packageName = ast.packageName.name,
        packageAlias = ast.packageAlias?.alias,
        entries = ast.entries.map { ImportEntry(it.name, it.alias?.alias) },
        sourceSection = ast.section
    )

fun convertBlock(ctx: ConversionContext, ast: ParseBlock): Block =
    Block(
        body = ast.body.map { convert(ctx, it) },
        sourceSection = ast.section
    )


fun convertBinaryOp(ctx: ConversionContext, ast: ParseBinaryOp): Expression =
    InfixOp(ast.op, convert(ctx, ast.left), convert(ctx, ast.right), ast.section)

fun convertCast(ctx: ConversionContext, ast: ParseCast): Expression =
    Cast(
        expression = convert(ctx, ast.value),
        targetType = ctx.resolveType(ast.typeRef),
        sourceSection = ast.section
    )

fun convertIs(ctx: ConversionContext, ast: ParseIs): Expression {
    return Is(
        value = convert(ctx, ast.value),
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
            val constructors = lookupResult.variants
            constructors?.firstOrNull { ctr -> ctr.variantName == it.typeOrVariant }
                ?.let { variant ->
                    val symbol = ctx.currentScope.getSymbol(it.value.name)!!
                    ifCtx.thenScope.addSymbol(
                        name = symbol.name,
                        type = valueType.withVariant(variant),
                        scope = SymbolType.Overwrite,
                        mutable = symbol.mutable
                    )

                }
        }
    }
}

fun convertNot(ctx: ConversionContext, ast: ParseNot): Expression =
    PrefixOp(
        op = "!",
        expr = convert(ctx, ast.value),
        sourceSection = ast.section
    )


