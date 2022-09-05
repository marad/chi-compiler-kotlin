package gh.marad.chi.core.exprbuilder

import gh.marad.chi.core.*
import gh.marad.chi.core.parser2.*
import gh.marad.chi.core.parser2.Program

// TODO: types powinno być generowane przez packageDescriptor, ten powinien mieć już definicje
// TODO: trzeba to usunąć z FooBar.getDefinedTypes
class ConversionContext(val namespace: GlobalCompilationNamespace, private val typeResolver: TypeResolver) {
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

    fun <T> withTypeParameters(typeParameterNames: Set<String>, f: () -> T): T =
        typeResolver.withTypeParameters(typeParameterNames, f)

    fun resolveType(typeRef: TypeRef, typeParameterNames: Set<String> = emptySet()): Type =
        typeResolver.resolveType(typeRef, typeParameterNames)
}

fun convertProgram(program: Program, namespace: GlobalCompilationNamespace): Block {
    val imports = program.imports.map { convertImportDefinition(it) }
    val packageDefinition = convertPackageDefinition(program.packageDefinition)
    val moduleName = packageDefinition?.moduleName ?: CompilationDefaults.defaultModule
    val packageName = packageDefinition?.packageName ?: CompilationDefaults.defaultPacakge

    val context = ConversionContext(namespace, TypeResolver.create(program))
    context.changeCurrentPackage(moduleName, packageName)

    // define imports and package functions/variant type constructors
    imports.forEach { context.imports.addImport(it) }

    val typeDefinitions = program.typeDefinitions.map { convertTypeDefinition(context, it) }

    FooBar.getFunctionDescriptors(program, context).forEach {
        context.currentScope.addSymbol(it.name, it.type, SymbolScope.Package, false)
    }

    val blockBody = mutableListOf<Expression>()
    packageDefinition?.let { blockBody.add(it) }
    blockBody.addAll(imports)
    blockBody.addAll(typeDefinitions)
    blockBody.addAll(program.functions.map { convert(context, it) })
    blockBody.addAll(program.topLevelCode.map { convert(context, it) })
    return Block(blockBody, null)
}

fun convertTypeDefinition(ctx: ConversionContext, definition: ParseVariantTypeDefinition): DefineVariantType {
    val typeParameterNames = definition.typeParameters.map { it.name }.toSet()
    return DefineVariantType(
        baseVariantType = ctx.resolveType(TypeNameRef(definition.typeName, null)) as VariantType,
        constructors = definition.variantConstructors.map {
            VariantTypeConstructor(
                name = it.name,
                fields = it.formalArguments.map { argument ->
                    VariantTypeField(
                        name = argument.name,
                        type = ctx.resolveType(argument.typeRef, typeParameterNames),
                        location = argument.section.asLocation()
                    )
                },
                location = it.section.asLocation()
            )
        },
        location = definition.section.asLocation(),
    )
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
    is ParseAssignment -> convertAssignment(ctx, ast)
    is ParseIndexedAssignment -> convertIndexedAssignment(ctx, ast)
    is ParseCast -> convertCast(ctx, ast)
    is ParseDotOp -> convertDotOp(ctx, ast)
    is ParseGroup -> convertGroup(ctx, ast)
    is ParseIfElse -> convertIfElse(ctx, ast)
    is ParseIndexOperator -> convertIndexOperator(ctx, ast)
    is ParseIs -> convertIs(ctx, ast)
    is ParseNot -> convertNot(ctx, ast)
    is ParseWhen -> convertWhen(ctx, ast)
    is ParseWhile -> convertWhile(ctx, ast)
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
                ).also { param ->
                    ctx.currentScope.addSymbol(param.name, param.type, SymbolScope.Argument, mutable = false)
                }
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
                    ).also { param ->
                        ctx.currentScope.addSymbol(param.name, param.type, SymbolScope.Argument, mutable = false)
                    }
                },
                returnType = ast.returnTypeRef?.let { ctx.resolveType(it, typeParameterNames) } ?: Type.unit,
                body = ctx.withTypeParameters(typeParameterNames) { convert(ctx, ast.body) as Block },
                location = ast.section.asLocation()
            )
        },
        mutable = false,
        expectedType = null,
        location = ast.section.asLocation()
    )
}

private fun convertPackageDefinition(ast: ParsePackageDefinition?): Package? =
    ast?.let {
        Package(ast.moduleName.name, ast.packageName.name, ast.section.asLocation())
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
    ).also {
        val scope = if (ctx.currentScope.isTopLevel) {
            SymbolScope.Package
        } else {
            SymbolScope.Local
        }
        ctx.currentScope.addSymbol(it.name, it.type, scope, it.mutable)
    }
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

fun convertFnCall(ctx: ConversionContext, ast: ParseFnCall): Expression {
    val function = convert(ctx, ast.function)
    return FnCall(
        enclosingScope = ctx.currentScope,
        name = ast.name, // FIXME: wydaje mi się, że to `name` nie jest potrzebne - zweryfikować w truffle
        function = function,
        callTypeParameters = ast.concreteTypeParameters.map { ctx.resolveType(it) },
        parameters = ast.arguments.map { convert(ctx, it) },
        location = ast.section.asLocation()
    )
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

fun convertCast(ctx: ConversionContext, ast: ParseCast): Expression =
    Cast(
        expression = convert(ctx, ast.value),
        targetType = ctx.resolveType(ast.typeRef),
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

fun convertGroup(ctx: ConversionContext, ast: ParseGroup): Expression =
    Group(
        value = convert(ctx, ast.value),
        location = ast.section.asLocation()
    )

fun convertIfElse(ctx: ConversionContext, ast: ParseIfElse): Expression =
    IfElse(
        condition = convert(ctx, ast.condition),
        thenBranch = convert(ctx, ast.thenBody),
        elseBranch = ast.elseBody?.let { convert(ctx, it) },
        location = ast.section.asLocation()
    )

fun convertIndexOperator(ctx: ConversionContext, ast: ParseIndexOperator): Expression =
    IndexOperator(
        variable = convert(ctx, ast.variable),
        index = convert(ctx, ast.index),
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

fun convertWhen(ctx: ConversionContext, ast: ParseWhen): Expression {
    val lastCase = ast.cases.last()
    val lastCaseAndElse = IfElse(
        condition = convert(ctx, lastCase.condition),
        thenBranch = convert(ctx, lastCase.body),
        elseBranch = ast.elseCase?.body?.let { convert(ctx, it) },
        location = lastCase.section.asLocation()
    )

    return ast.cases.dropLast(1).foldRight<ParseWhenCase, Expression>(lastCaseAndElse) { case, acc ->
        IfElse(
            condition = convert(ctx, case.condition),
            thenBranch = convert(ctx, case.body),
            elseBranch = acc,
            location = case.section.asLocation()
        )
    }
}

fun convertWhile(ctx: ConversionContext, ast: ParseWhile): Expression =
    WhileLoop(
        condition = convert(ctx, ast.condition),
        loop = convert(ctx, ast.body),
        location = ast.section.asLocation()
    )

