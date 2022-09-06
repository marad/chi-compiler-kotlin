package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.asLocation
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.parser2.*

fun convertVariableRead(ctx: ConversionContext, ast: ParseVariableRead): Expression {
    val lookup = ctx.lookup(ast.variableName)
    return VariableAccess(
        moduleName = lookup.moduleName,
        packageName = lookup.packageName,
        definitionScope = lookup.scope, // TODO: czy ten compilation scope jest potrzebny?
        name = lookup.name,
        location = ast.section.asLocation()
    )
}

fun convertNameDeclaration(ctx: ConversionContext, ast: ParseNameDeclaration): Expression {
    return NameDeclaration(
        enclosingScope = ctx.currentScope,
        name = ast.name.name,
        value = convert(ctx, ast.value),
        mutable = ast.mutable,
        expectedType = ast.typeRef?.let { ctx.resolveType(it) },
        location = ast.section?.asLocation()
    ).also {
        val scope = if (ctx.currentScope.isTopLevel) {
            SymbolScope.Package
        } else {
            SymbolScope.Local
        }
        ctx.currentScope.addSymbol(it.name, it.type, scope, it.mutable)
    }
}

fun convertAssignment(ctx: ConversionContext, ast: ParseAssignment): Expression =
    // TODO czy tutaj nie lepiej mieć zamiast `name` VariableAccess i mieć tam nazwę i pakiet?
    Assignment(
        definitionScope = ctx.currentScope,
        name = ast.variableName,
        value = convert(ctx, ast.value),
        location = ast.section.asLocation()
    )

fun convertIndexedAssignment(ctx: ConversionContext, ast: ParseIndexedAssignment): Expression =
    IndexedAssignment(
        variable = convert(ctx, ast.variable),
        index = convert(ctx, ast.index),
        value = convert(ctx, ast.value),
        location = ast.section.asLocation()
    )

fun convertIndexOperator(ctx: ConversionContext, ast: ParseIndexOperator): Expression =
    IndexOperator(
        variable = convert(ctx, ast.variable),
        index = convert(ctx, ast.index),
        location = ast.section.asLocation()
    )

fun convertDotOp(ctx: ConversionContext, ast: ParseDotOp): Expression {
    val pkg = ctx.imports.lookupPackage(ast.receiverName)

    if (pkg != null) {
        return VariableAccess(
            pkg.module, pkg.pkg,
            ctx.namespace.getOrCreatePackage(pkg.module, pkg.pkg).scope,
            ast.memberName,
            ast.section.asLocation()
        )
    }

    val receiver = convert(ctx, ast.receiver)
    val member = convert(ctx, ast.member)

    // TODO - to jest jedyny powód dla którego potrzebujemy compilation scope tak na prawdę
    if (receiver.type.isCompositeType() && member is Assignment) {
        return FieldAssignment(
            receiver, member.name, member.value, ast.section.asLocation()
        )
    }

    return FieldAccess(
        receiver,
        ast.memberName,
        ast.section.asLocation(),
        ast.member.section.asLocation(),
    )
}
