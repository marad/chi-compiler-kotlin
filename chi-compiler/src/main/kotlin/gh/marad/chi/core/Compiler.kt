package gh.marad.chi.core

import ChiLexer
import ChiParser
import ChiParserBaseVisitor
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode


data class CompilationResult(
    val messages: List<Message>,
    val program: Program,
) {
    fun hasErrors(): Boolean = messages.any { it.level == Level.ERROR }
    fun errors() = messages.filter { it.level == Level.ERROR }
}

object Compiler {
    /**
     * Compiles source code and produces compilation result that
     * contains AST and compilation messages.
     *
     * @param source Chi source code.
     * @param namespace Namespace to use for compilation
     */
    @JvmStatic
    fun compile(source: String, namespace: GlobalCompilationNamespace): CompilationResult {
        val (program, parsingMessages) = parseProgram(source, namespace)
        return if (parsingMessages.isNotEmpty()) {
            CompilationResult(parsingMessages, program)
        } else {
            val messages = analyze(program)
            CompilationResult(messages, program)
        }
    }

    @JvmStatic
    fun formatCompilationMessage(source: String, message: Message): String {
        val location = message.location
        val sb = StringBuilder()
        if (location != null) {
            val sourceLine = source.lines()[location.start.line - 1]
            sb.appendLine(sourceLine)
            repeat(location.start.column) { sb.append(' ') }
            sb.append("^ ")
        }
        sb.append(message.message)
        return sb.toString()
    }
}

internal fun parseProgram(source: String, namespace: GlobalCompilationNamespace): Pair<Program, List<Message>> {
    val errorListener = MessageCollectingErrorListener()
    val charStream = CharStreams.fromString(source)
    val lexer = ChiLexer(charStream)
    lexer.removeErrorListeners()
    lexer.addErrorListener(errorListener)
    val tokenStream = CommonTokenStream(lexer)
    val parser = ChiParser(tokenStream)
    parser.errorHandler = DefaultErrorStrategy()
    parser.removeErrorListeners()
    parser.addErrorListener(errorListener)
    val visitor = AntlrToAstVisitor(namespace)
    val program = if (errorListener.getMessages().isNotEmpty()) {
        Program(emptyList())
    } else {
        visitor.visitProgram(parser.program()) as Program
    }
    return Pair(
        automaticallyCastCompatibleTypes(program) as Program,
        errorListener.getMessages()
    )
}

internal class AntlrToAstVisitor(private val namespace: GlobalCompilationNamespace) :
    ChiParserBaseVisitor<Expression>() {

    private val imports = namespace.createCompileTimeImports()

    private var currentPackageDescriptor: PackageDescriptor = namespace.getDefaultPackage()
    private var currentScope = currentPackageDescriptor.scope

    override fun visitProgram(ctx: ChiParser.ProgramContext): Expression {
        ctx.removeLastChild() // remove EOF
        val exprs = ctx.children.mapNotNull { it.accept(this) }
        return Program(exprs)
    }

    override fun visitPackage_definition(ctx: ChiParser.Package_definitionContext): Expression {
        val moduleName = ctx.module_name()?.text ?: ""
        val packageName = ctx.package_name()?.text ?: ""
        currentPackageDescriptor = namespace.getOrCreatePackage(moduleName, packageName)
        currentScope = currentPackageDescriptor.scope
        return Package(moduleName, packageName, makeLocation(ctx))
    }

    override fun visitImport_definition(ctx: ChiParser.Import_definitionContext): Expression {
        val import = Import(
            moduleName = ctx.module_name()?.text ?: "",
            packageName = ctx.package_name()?.text ?: "",
            packageAlias = ctx.package_import_alias()?.text,
            entries = ctx.import_entry().map { entryCtx ->
                ImportEntry(
                    name = entryCtx.import_name().text,
                    alias = entryCtx.name_import_alias()?.text,
                )
            },
            location = makeLocation(ctx),
        )
        imports.addImport(import)
        return import
    }

    override fun visitVariantTypeDefinition(ctx: ChiParser.VariantTypeDefinitionContext): Expression {
        val simpleTypeName = ctx.typeName.text
        val genericTypeParameters = ctx.generic_type_definitions()
            ?.let { readGenericTypeParameterDefinitions(it) }
            ?: emptyList()
        val location = makeLocation(ctx)
        val moduleName = currentPackageDescriptor.moduleName
        val packageName = currentPackageDescriptor.packageName
        // To allow reading recurring types I first create temporary descriptor without variants.
        // This is needed so that the defined type can be properly recognized for fields.
        // It's later replaced by fully defined type descriptor
        val temporaryTypeWithoutVariants =
            VariantTypeDefinition(moduleName, packageName, simpleTypeName, genericTypeParameters, emptyList())
        currentPackageDescriptor.variantTypes.defineType(temporaryTypeWithoutVariants)

        val variantConstructors = ctx.variantTypeConstructors()?.variantTypeConstructor()?.map {
            readVariantTypeConstructor(it)
        } ?: emptyList()
        val variants = variantConstructors.map { VariantType.Variant(it.name, it.fields) }
        val baseType = VariantTypeDefinition(moduleName, packageName, simpleTypeName, genericTypeParameters, variants)
        currentPackageDescriptor.variantTypes.defineType(baseType)
        variantConstructors.forEach { constructor ->
            val variant = VariantType.Variant(constructor.name, constructor.fields)
            val type = baseType.construct(variant)
            if (constructor.fields.isNotEmpty()) {
                currentPackageDescriptor.scope.addSymbol(
                    name = constructor.name,
                    type = if (genericTypeParameters.isEmpty()) {
                        Type.fn(type, *constructor.fields.map { it.type }.toTypedArray())
                    } else {
                        Type.genericFn(
                            genericTypeParameters,
                            type,
                            *constructor.fields.map { it.type }.toTypedArray()
                        )
                    },
                    scope = SymbolScope.Package,
                    false
                )
            } else {
                currentPackageDescriptor.scope.addSymbol(
                    name = constructor.name,
                    type = type,
                    scope = SymbolScope.Package,
                )
            }
        }
        return DefineVariantType(
            moduleName,
            packageName,
            simpleTypeName,
            variantConstructors,
            location
        )
    }

    private fun readVariantTypeConstructor(ctx: ChiParser.VariantTypeConstructorContext): VariantTypeConstructor {
        val constructorName = ctx.variantName.text
        val fields = ctx.func_argument_definitions()?.argumentsWithTypes()?.argumentWithType()?.map {
            val name = it.ID().text
            val type = readType(it.type())
            VariantTypeField(name, type, makeLocation(it))
        } ?: emptyList()
        return VariantTypeConstructor(constructorName, fields, makeLocation(ctx))
    }

    override fun visitName_declaration(ctx: ChiParser.Name_declarationContext): Expression {
        val symbolName = ctx.ID().text
        val value = ctx.expression().accept(this)
        val mutable = ctx.VAR() != null
        val expectedType = ctx.type()?.let { readType(it) }
        val location = makeLocation(ctx)
        return createNameDeclaration(symbolName, value, mutable, expectedType, location)
    }

    private fun createNameDeclaration(
        symbolName: String,
        value: Expression,
        mutable: Boolean,
        expectedType: Type?,
        location: Location
    ): NameDeclaration {
        val scope = if (currentScope.isTopLevel) {
            SymbolScope.Package
        } else {
            SymbolScope.Local
        }

        currentScope.addSymbol(symbolName, value.type, scope, mutable)
        return NameDeclaration(currentScope, symbolName, value, mutable, expectedType, location)
    }

    private fun readType(ctx: ChiParser.TypeContext): Type {
        val primitiveType = ctx.ID()?.let { maybePrimitiveType(it.text) }
        return if (primitiveType != null) {
            return primitiveType
        } else if (ctx.ID() != null) {
            val typeName = ctx.ID().text
            val type = currentPackageDescriptor.variantTypes.get(typeName)?.getWithSingleOrNoVariant()
                ?: imports.lookupType(typeName)?.getWithSingleOrNoVariant()

            if (type != null) {
                return type
            } else {
                Type.typeParameter(ctx.ID().text)
            }
        } else if (ctx.generic_type() != null) {
            val genericTypeName = ctx.generic_type().name.text
            val genericTypeParameters = ctx.generic_type().type().map { readType(it) }
            val variantType = currentPackageDescriptor.variantTypes.get(genericTypeName)?.getWithSingleOrNoVariant()
                ?: imports.lookupType(genericTypeName)?.getWithSingleOrNoVariant()

            if (genericTypeName == "array") {
                return Type.array(genericTypeParameters.first())
            } else if (variantType != null) {
                return variantType
            } else {
                TODO("Unknown generic type '$genericTypeName' with parameters $genericTypeParameters")
            }
        } else {
            // read function type
            val argTypes = ctx.type().map { readType(it) }
            val returnType = readType(ctx.func_return_type().type())
            FnType(emptyList(), argTypes, returnType)
        }
    }

    private fun maybePrimitiveType(name: String): Type? = Type.primitiveTypes.find { it.name == name }

    override fun visitGroupExpr(ctx: ChiParser.GroupExprContext): Expression {
        return Group(visit(ctx.expression()), makeLocation(ctx))
    }

    override fun visitFunc(ctx: ChiParser.FuncContext): Expression {
        return withNewScope {
            val fnParams = readFunctionParams(ctx.func_argument_definitions())
            val returnType = ctx.func_return_type()?.type()?.let { readType(it) } ?: Type.unit
            val block = visitBlock(ctx.func_body().block()) as Block
            Fn(currentScope, emptyList(), fnParams, returnType, block, makeLocation(ctx))
        }
    }

    override fun visitFuncWithName(ctx: ChiParser.FuncWithNameContext): Expression {
        val func = withNewScope {
            val fnParams = readFunctionParams(ctx.func_with_name().arguments)
            val returnType = ctx.func_with_name().func_return_type()?.type()?.let { readType(it) } ?: Type.unit
            val block = visitBlock(ctx.func_with_name().func_body().block()) as Block
            val genericTypeParameters =
                readGenericTypeParameterDefinitions(ctx.func_with_name().generic_type_definitions())
            Fn(currentScope, genericTypeParameters, fnParams, returnType, block, makeLocation(ctx))
        }
        return createNameDeclaration(
            ctx.func_with_name().funcName.text,
            func,
            false,
            func.type,
            makeLocation(ctx)
        )
    }

    private fun readFunctionParams(ctx: ChiParser.Func_argument_definitionsContext): List<FnParam> {
        return if (ctx.argumentsWithTypes() != null) {
            ctx.argumentsWithTypes().argumentWithType().map {
                val name = it.ID().text
                val type = readType(it.type())
                val location = makeLocation(it.ID().symbol, it.type().stop)
                currentScope.addSymbol(name, type, SymbolScope.Argument)
                FnParam(name, type, location)
            }
        } else {
            emptyList()
        }
    }

    private fun readGenericTypeParameterDefinitions(ctx: ChiParser.Generic_type_definitionsContext?): List<GenericTypeParameter> {
        return ctx?.ID()?.map {
            GenericTypeParameter(it.text)
        } ?: emptyList()
    }

    override fun visitBlock(ctx: ChiParser.BlockContext): Expression {
        val body = ctx.expression().map { visit(it) }
        return Block(body, makeLocation(ctx))
    }

    private fun Token.toLocationPoint() = LocationPoint(line, charPositionInLine)

    private fun makeLocation(ctx: ParserRuleContext) =
        makeLocation(ctx.start, ctx.stop)

    private fun makeLocation(start: Token, stop: Token?) =
        Location(
            start = start.toLocationPoint(),
            end = stop?.toLocationPoint() ?: start.toLocationPoint(),
            startIndex = start.startIndex,
            endIndex = stop?.stopIndex ?: start.stopIndex
        )

    override fun visitTerminal(node: TerminalNode): Expression? {

        val location = makeLocation(node.symbol, node.symbol)
        return when (node.symbol.type) {
            ChiLexer.NUMBER -> {
                if (node.text.contains(".")) {
                    Atom(node.text, Type.floatType, location)
                } else {
                    Atom(node.text, Type.intType, location)
                }
            }
            ChiLexer.ID -> {
                val import = imports.lookupName(node.text)
                if (import != null) {
                    VariableAccess(
                        import.module,
                        import.pkg,
                        definitionScope = namespace.getOrCreatePackage(import.module, import.pkg).scope,
                        import.name,
                        location
                    )
                } else {
                    VariableAccess(
                        currentPackageDescriptor.moduleName,
                        currentPackageDescriptor.packageName,
                        currentScope,
                        node.text,
                        location
                    )
                }
            }
            ChiLexer.TRUE -> Atom.t(location)
            ChiLexer.FALSE -> Atom.f(location)
            ChiLexer.NEWLINE -> null
            else -> {
                TODO("Unsupported type ${node.symbol.type}")
            }
        }
    }

    override fun visitAssignment(ctx: ChiParser.AssignmentContext): Expression {
        val name = ctx.ID().text
        val value = ctx.value.accept(this)
        return Assignment(currentScope, name, value, makeLocation(ctx))
    }

    override fun visitFnCallExpr(ctx: ChiParser.FnCallExprContext): Expression {
        val calledName = ctx.expression().text
        val function = visit(ctx.expression())
        val callTypeParameters = readCallGenericParameters(ctx.callGenericParameters())
        val parameters = ctx.expr_comma_list().expression().map { visit(it) }
        return FnCall(currentScope, calledName, function, callTypeParameters, parameters, makeLocation(ctx))
    }

    private fun readCallGenericParameters(ctx: ChiParser.CallGenericParametersContext?): List<Type> {
        return ctx?.type()?.map { readType(it) } ?: emptyList()
    }


    override fun visitIf_expr(ctx: ChiParser.If_exprContext): Expression {
        val condition = ctx.condition().expression().accept(this)
        val thenPart = visit(ctx.then_expr().expression())
        val elsePart = ctx.else_expr()?.expression()?.let { visit(it) }
        return IfElse(
            condition = condition,
            thenBranch = thenPart,
            elseBranch = elsePart,
            makeLocation(ctx)
        )
    }

    override fun visitNotOp(ctx: ChiParser.NotOpContext): Expression {
        val opTerminal = ctx.NOT()
        val expr = ctx.expression().accept(this)
        return PrefixOp(opTerminal.text, expr, makeLocation(ctx))
    }

    override fun visitBinOp(ctx: ChiParser.BinOpContext): Expression {
        val opTerminal = ctx.ADD_SUB()
            ?: ctx.MUL()
            ?: ctx.DIV()
            ?: ctx.MOD()
            ?: ctx.and()
            ?: ctx.COMP_OP()
            ?: ctx.or()
            ?: ctx.BIT_AND()
            ?: ctx.BIT_OR()
            ?: ctx.BIT_SHL()
            ?: ctx.BIT_SHR()
        val op = opTerminal.text
        val left = ctx.expression(0).accept(this)
        val right = ctx.expression(1).accept(this)
        return InfixOp(op, left, right, makeLocation(ctx))
    }

    override fun visitCast(ctx: ChiParser.CastContext): Expression {
        val targetType = readType(ctx.type())
        val expression = ctx.expression().accept(this)
        return Cast(expression, targetType, makeLocation(ctx))
    }

    override fun visitDotOp(ctx: ChiParser.DotOpContext): Expression {
        val pkg = imports.lookupPackage(ctx.receiver.text)
        if (pkg != null) {
            return VariableAccess(
                pkg.module,
                pkg.pkg,
                namespace.getOrCreatePackage(pkg.module, pkg.pkg).scope,
                ctx.member.text,
                makeLocation(ctx)
            )
        }

        val receiver = visit(ctx.receiver)
        val member = visit(ctx.member)

        if (receiver.type.isCompositeType() && member is Assignment) {
            return FieldAssignment(
                receiver, member.name, member.value, makeLocation(ctx)
            )
        }

        return FieldAccess(
            receiver,
            ctx.member.text,
            makeLocation(ctx),
            makeLocation(ctx.member)
        )
    }

    override fun visitString(ctx: ChiParser.StringContext): Expression {
        val value = ctx.string_part().joinToString("") { it.text }
        return Atom.string(value, makeLocation(ctx))
    }

    override fun visitWhileLoopExpr(ctx: ChiParser.WhileLoopExprContext): Expression {
        val condition = visit(ctx.expression())
        val loop = visit(ctx.block())
        return WhileLoop(condition, loop, makeLocation(ctx))
    }

    override fun visitIndexOperator(ctx: ChiParser.IndexOperatorContext): Expression {
        val variable = ctx.variable.accept(this)
        val index = ctx.index.accept(this)
        return IndexOperator(variable, index, makeLocation(ctx))
    }

    override fun visitIndexedAssignment(ctx: ChiParser.IndexedAssignmentContext): Expression {
        val variable = ctx.variable.accept(this)
        val index = ctx.index.accept(this)
        val value = ctx.value.accept(this)
        return IndexedAssignment(variable, index, value, makeLocation(ctx))
    }

    private fun <T> withNewScope(f: () -> T): T {
        val parentScope = currentScope
        currentScope = CompilationScope(parentScope)
        try {
            return f()
        } finally {
            currentScope = parentScope
        }
    }
}
