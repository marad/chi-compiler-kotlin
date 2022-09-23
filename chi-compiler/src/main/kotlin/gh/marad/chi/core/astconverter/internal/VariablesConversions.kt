package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.*

fun convertVariableRead(ctx: ConversionContext, ast: ParseVariableRead): Expression {
    val lookup = ctx.lookup(ast.variableName)
    return VariableAccess(
        moduleName = lookup.moduleName,
        packageName = lookup.packageName,
        definitionScope = lookup.scope, // TODO: czy ten compilation scope jest potrzebny?
        name = lookup.name,
        sourceSection = ast.section
    )
}

fun convertNameDeclaration(ctx: ConversionContext, ast: ParseNameDeclaration): Expression {
    return NameDeclaration(
        enclosingScope = ctx.currentScope,
        name = ast.name.name,
        value = convert(ctx, ast.value),
        mutable = ast.mutable,
        expectedType = ast.typeRef?.let { ctx.resolveType(it) },
        sourceSection = ast.section
    ).also {
        ctx.currentScope.addSymbol(it.name, it.type, SymbolType.Local, it.mutable)
    }
}

fun convertAssignment(ctx: ConversionContext, ast: ParseAssignment): Expression =
    // TODO czy tutaj nie lepiej mieć zamiast `name` VariableAccess i mieć tam nazwę i pakiet?
    Assignment(
        definitionScope = ctx.currentScope,
        name = ast.variableName,
        value = convert(ctx, ast.value),
        sourceSection = ast.section
    )

fun convertIndexedAssignment(ctx: ConversionContext, ast: ParseIndexedAssignment): Expression =
    IndexedAssignment(
        variable = convert(ctx, ast.variable),
        index = convert(ctx, ast.index),
        value = convert(ctx, ast.value),
        sourceSection = ast.section
    )

fun convertIndexOperator(ctx: ConversionContext, ast: ParseIndexOperator): Expression =
    IndexOperator(
        variable = convert(ctx, ast.variable),
        index = convert(ctx, ast.index),
        sourceSection = ast.section
    )

fun convertFieldAccess(ctx: ConversionContext, ast: ParseFieldAccess): Expression {
    val pkg = ctx.imports.lookupPackage(ast.receiverName)

    if (pkg != null) {
        return VariableAccess(
            pkg.module, pkg.pkg,
            ctx.namespace.getOrCreatePackage(pkg.module, pkg.pkg).scope,
            ast.memberName,
            ast.section
        )
    }

    val receiver = convert(ctx, ast.receiver)
    return FieldAccess(
        receiver,
        ast.memberName,
        ast.section,
        ast.memberSection,
    )
}

fun convertMethodInvocation(ctx: ConversionContext, ast: ParseMethodInvocation): Expression {
    val pkg = ctx.imports.lookupPackage(ast.receiverName)

    val function = if (pkg != null) {
        VariableAccess(
            pkg.module, pkg.pkg,
            ctx.namespace.getOrCreatePackage(pkg.module, pkg.pkg).scope,
            ast.methodName,
            ast.section
        )
    } else {

        val methodLookup = ctx.lookup(ast.methodName)
        val methodPkg = ctx.namespace.getOrCreatePackage(methodLookup.moduleName, methodLookup.packageName)
        val method = methodPkg.scope
            .getSymbol(ast.methodName) ?: TODO("Unknown method: ${ast.methodName} called on ${ast.receiverName}")

        VariableAccess(
            methodLookup.moduleName,
            methodLookup.packageName,
            methodPkg.scope,
            method.name,
            ast.memberSection
        )
    }

    val receiver = convert(ctx, ast.receiver)
    val convertedArguments = ast.arguments.map { convert(ctx, it) }

    val arguments = if (pkg != null) {
        convertedArguments
    } else {
        listOf(receiver) + convertedArguments
    }

    return FnCall(
        function = function,
        callTypeParameters = ast.concreteTypeParameters.map { ctx.resolveType(it) },
        parameters = arguments,
        ast.section
    )
}


fun convertFieldAssignment(ctx: ConversionContext, ast: ParseFieldAssignment): Expression {
    return FieldAssignment(
        receiver = convert(ctx, ast.receiver),
        fieldName = ast.memberName,
        value = convert(ctx, ast.value),
        sourceSection = ast.section
    )
}

