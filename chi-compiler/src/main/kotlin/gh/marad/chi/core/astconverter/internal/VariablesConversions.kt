package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.*

fun convertVariableRead(ctx: ConversionContext, ast: ParseVariableRead): Expression {
    val lookup = ctx.lookup(ast.variableName)
    return VariableAccess(
        isModuleLocal = lookup.moduleName == ctx.currentModule,
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
        public = ast.public,
        name = ast.symbol.name,
        value = convert(ctx, ast.value),
        mutable = ast.mutable,
        expectedType = ast.typeRef?.let { ctx.resolveType(it) },
        sourceSection = ast.section
    ).also {
        ctx.currentScope.addSymbol(it.name, it.type, SymbolType.Local, public = it.public, mutable = it.mutable)
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
            isModuleLocal = pkg.module == ctx.currentModule,
            moduleName = pkg.module,
            packageName = pkg.pkg,
            definitionScope = ctx.namespace.getOrCreatePackage(pkg.module, pkg.pkg).scope,
            name = ast.memberName,
            sourceSection = ast.section
        )
    }

    val receiver = convert(ctx, ast.receiver)
    return FieldAccess(
        receiver,
        ast.memberName,
        typeIsModuleLocal = ctx.currentModule == receiver.type.moduleName,
        ast.section,
        ast.memberSection,
    )
}

fun convertMethodInvocation(ctx: ConversionContext, ast: ParseMethodInvocation): Expression {
    val receiver = convert(ctx, ast.receiver)
    val pkg = ctx.imports.lookupPackage(ast.receiverName)

    val function = sequenceOf(
        {
            if (pkg != null) {
                VariableAccess(
                    isModuleLocal = pkg.module == ctx.currentModule,
                    moduleName = pkg.module,
                    packageName = pkg.pkg,
                    definitionScope = ctx.namespace.getOrCreatePackage(pkg.module, pkg.pkg).scope,
                    name = ast.methodName,
                    sourceSection = ast.memberSection
                )
            } else null
        },
        {
            val scope = ctx.namespace.getOrCreatePackage(receiver.type.moduleName, receiver.type.packageName).scope
            if (scope.containsSymbol(ast.methodName)) {
                VariableAccess(
                    isModuleLocal = receiver.type.moduleName == ctx.currentModule,
                    moduleName = receiver.type.moduleName,
                    packageName = receiver.type.packageName,
                    definitionScope = scope,
                    name = ast.methodName,
                    sourceSection = ast.memberSection
                )
            } else null
        },
        {
            val methodLookup = ctx.lookup(ast.methodName)
            val methodPkg = ctx.namespace.getOrCreatePackage(methodLookup.moduleName, methodLookup.packageName)
            VariableAccess(
                isModuleLocal = methodLookup.moduleName == ctx.currentModule,
                moduleName = methodLookup.moduleName,
                packageName = methodLookup.packageName,
                definitionScope = methodPkg.scope,
                name = ast.methodName,
                sourceSection = ast.memberSection
            )
        }
    ).map { it() }.filterNotNull().first()

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
        ast.memberSection
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

