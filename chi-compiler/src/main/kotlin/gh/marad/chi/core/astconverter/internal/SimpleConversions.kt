package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.asLocation
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.parser2.*


fun convertAtom(value: BoolValue) =
    if (value.value) Atom.t(value.section.asLocation())
    else Atom.f(value.section.asLocation())

fun convertAtom(value: FloatValue) =
    Atom.float(value.value, value.section.asLocation())

fun convertAtom(ast: LongValue) =
    Atom.int(ast.value, ast.section.asLocation())

fun convertAtom(ast: StringValue) =
    Atom.string(ast.value, ast.section.asLocation())

fun convertPackageDefinition(ast: ParsePackageDefinition?): Package? =
    ast?.let {
        Package(ast.moduleName.name, ast.packageName.name, ast.section.asLocation())
    }

fun convertImportDefinition(ast: ParseImportDefinition): Import =
    Import(
        moduleName = ast.moduleName.name,
        packageName = ast.packageName.name,
        packageAlias = ast.packageAlias?.alias,
        entries = ast.entries.map { ImportEntry(it.name, it.alias?.alias) },
        location = ast.section.asLocation()
    )

fun convertBlock(ctx: ConversionContext, ast: ParseBlock): Expression =
    Block(
        body = ast.body.map { convert(ctx, it) },
        location = ast.section.asLocation()
    )


fun convertBinaryOp(ctx: ConversionContext, ast: ParseBinaryOp): Expression =
    InfixOp(ast.op, convert(ctx, ast.left), convert(ctx, ast.right), ast.section.asLocation())

fun convertCast(ctx: ConversionContext, ast: ParseCast): Expression =
    Cast(
        expression = convert(ctx, ast.value),
        targetType = ctx.resolveType(ast.typeRef),
        location = ast.section.asLocation()
    )

fun convertIs(ctx: ConversionContext, ast: ParseIs): Expression =
    Is(
        value = convert(ctx, ast.value),
        variantName = ast.typeName,
        location = ast.section.asLocation()
    )

fun convertNot(ctx: ConversionContext, ast: ParseNot): Expression =
    PrefixOp(
        op = "!",
        expr = convert(ctx, ast.value),
        location = ast.section.asLocation()
    )


