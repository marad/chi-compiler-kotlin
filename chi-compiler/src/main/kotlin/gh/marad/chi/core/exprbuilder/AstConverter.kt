package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.*
import gh.marad.chi.core.parser2.*
import gh.marad.chi.core.parser2.Program

class ConversionContext(val namespace: GlobalCompilationNamespace, val types: Map<String, Type>) {
    val imports = namespace.createCompileTimeImports()
    var currentPackageDescriptor = namespace.getDefaultPackage()
        private set
    var currentScope = currentPackageDescriptor.scope
        private set

    val currentModule: String get() = currentPackageDescriptor.moduleName
    val currentPackage: String get() = currentPackageDescriptor.packageName

    fun changeCurrentPackage(moduleName: String, packageName: String) {
        currentPackageDescriptor = namespace.getOrCreatePackage(moduleName, packageName)
        currentScope = currentPackageDescriptor.scope
    }

    fun <T> withNewScope(f: () -> T): T {
        val parentScope = currentScope
        currentScope = CompilationScope(parentScope)
        try {
            return f()
        } finally {
            currentScope = parentScope
        }
    }

    data class LookupResult(
        val moduleName: String,
        val packageName: String,
        val scope: CompilationScope,
        val name: String
    )

    fun lookup(name: String): LookupResult {
        val imported = imports.lookupName(name)
        return if (imported != null) {
            LookupResult(
                imported.module,
                imported.pkg,
                namespace.getOrCreatePackage(imported.module, imported.pkg).scope,
                imported.name
            )
        } else {
            LookupResult(currentModule, currentPackage, currentScope, name)
        }
    }

    fun resolveType(typeRef: TypeRef, typeParameterNames: Set<String> = emptySet()): Type =
        FooBar.resolveType(typeRef, types, typeParameterNames)
}

fun convertProgram(program: Program, namespace: GlobalCompilationNamespace): Block {
    val imports = program.imports.map { convertImportDefinition(it) }
    val packageDefinition = convertPackageDefinition(program.packageDefinition)

    val context = ConversionContext(namespace, FooBar.basicTypes + FooBar.getDefinedTypes(program))
    context.changeCurrentPackage(packageDefinition.moduleName, packageDefinition.packageName)

    // define imports and package functions/variant type constructors
    imports.forEach { context.imports.addImport(it) }
    FooBar.getFunctionDescriptors(program).forEach {
        context.currentScope.addSymbol(it.name, it.type, SymbolScope.Package, false)
    }

    val blockBody = mutableListOf<Expression>()
    blockBody.add(packageDefinition)
    blockBody.addAll(imports)
    blockBody.addAll(program.functions.map { convert(context, it) })
    blockBody.addAll(program.topLevelCode.map { convert(context, it) })
    return Block(blockBody, null)
}

fun convert(ctx: ConversionContext, ast: ParseAst): Expression = when (ast) {
    is ParseFunc -> convertFunc(ctx, ast)
    is ParseFuncWithName -> convertFuncWithName(ctx, ast)
    is ParseNameDeclaration -> convertNameDeclaration(ctx, ast)
    is ParseBlock -> convertBlock(ctx, ast)
    is ParseVariableRead -> convertVariableRead(ctx, ast)
    is LongValue -> convertAtom(ast)
    is FloatValue -> convertAtom(ast)
    is BoolValue -> convertAtom(ast)
    is StringValue -> convertAtom(ast)
    is ParseBinaryOp -> convertBinaryOp(ctx, ast)
    is ParseFnCall -> convertFnCall(ctx, ast)
    is ParseAssignment -> TODO()
    is ParseCast -> TODO()
    is ParseDotOp -> TODO()
    is ParseGroup -> TODO()
    is ParseIfElse -> TODO()
    is ParseIndexOperator -> TODO()
    is ParseIs -> TODO()
    is ParseNot -> TODO()
    is ParseWhen -> TODO()
    is ParseWhile -> TODO()
    else -> TODO("Unsupported conversion of AST element $ast")
}

fun convertFunc(ctx: ConversionContext, ast: ParseFunc): Expression =
    ctx.withNewScope {
        Fn(
            fnScope = ctx.currentScope,
            genericTypeParameters = emptyList(),
            parameters = ast.formalArguments.map {
                FnParam(
                    it.name,
                    ctx.resolveType(it.typeRef),
                    it.section.asLocation()
                )
            },
            returnType = ast.returnTypeRef.let { ctx.resolveType(it) },
            body = convert(ctx, ast.body) as Block,
            location = ast.section.asLocation()
        )
    }

fun convertFuncWithName(ctx: ConversionContext, ast: ParseFuncWithName): Expression {
    val typeParameterNames = ast.typeParameters.map { it.name }.toSet()
    return NameDeclaration(
        enclosingScope = ctx.currentScope,
        name = ast.name,
        value = ctx.withNewScope {
            Fn(
                fnScope = ctx.currentScope,
                genericTypeParameters = ast.typeParameters.map { GenericTypeParameter(it.name) },
                parameters = ast.formalArguments.map {
                    FnParam(
                        it.name,
                        ctx.resolveType(it.typeRef, typeParameterNames),
                        it.section.asLocation()
                    )
                },
                returnType = ast.returnTypeRef?.let { ctx.resolveType(it, typeParameterNames) } ?: Type.unit,
                body = convert(ctx, ast.body) as Block,
                location = ast.section.asLocation()
            )
        },
        mutable = false,
        expectedType = null,
        location = ast.section.asLocation()
    )
}

private fun convertPackageDefinition(ast: ParsePackageDefinition?): gh.marad.chi.core.Package =
    if (ast == null) {
        gh.marad.chi.core.Package(
            CompilationDefaults.defaultModule,
            CompilationDefaults.defaultPacakge,
            null
        )
    } else {
        gh.marad.chi.core.Package(ast.moduleName.name, ast.packageName.name, ast.section.asLocation())
    }

private fun convertImportDefinition(ast: ParseImportDefinition): Import =
    Import(
        moduleName = ast.moduleName.name,
        packageName = ast.packageName.name,
        packageAlias = ast.packageAlias?.alias,
        entries = ast.entries.map { ImportEntry(it.name, it.alias?.alias) },
        location = ast.section.asLocation()
    )

private fun convertNameDeclaration(ctx: ConversionContext, ast: ParseNameDeclaration): Expression {
    return NameDeclaration(
        enclosingScope = ctx.currentScope,
        name = ast.name.name,
        value = convert(ctx, ast.value),
        mutable = ast.mutable,
        expectedType = ast.typeRef?.let { ctx.resolveType(it) },
        location = ast.section?.asLocation()
    )
}

fun convertBlock(ctx: ConversionContext, ast: ParseBlock): Expression =
    Block(
        body = ast.body.map { convert(ctx, it) },
        location = ast.section.asLocation()
    )

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

fun convertAtom(value: BoolValue) =
    if (value.value) Atom.t(value.section.asLocation())
    else Atom.f(value.section.asLocation())

fun convertAtom(value: FloatValue) =
    Atom.float(value.value, value.section.asLocation())

fun convertAtom(ast: LongValue) =
    Atom.int(ast.value, ast.section.asLocation())

fun convertAtom(ast: StringValue) =
    Atom.string(ast.value, ast.section.asLocation())

fun convertBinaryOp(ctx: ConversionContext, ast: ParseBinaryOp): Expression =
    InfixOp(ast.op, convert(ctx, ast.left), convert(ctx, ast.right), ast.section.asLocation())

fun convertFnCall(ctx: ConversionContext, ast: ParseFnCall): Expression =
    FnCall(
        enclosingScope = ctx.currentScope,
        name = ast.name, // FIXME: wydaje mi się, że to `name` nie jest potrzebne - zweryfikować w truffle
        function = convert(ctx, ast.function),
        callTypeParameters = ast.concreteTypeParameters.map { ctx.resolveType(it) },
        parameters = ast.arguments.map { convert(ctx, it) },
        location = ast.section.asLocation()
    )

