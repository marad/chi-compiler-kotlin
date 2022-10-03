package gh.marad.chi.core.astconverter

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.internal.*
import gh.marad.chi.core.namespace.GlobalCompilationNamespace
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.*
import gh.marad.chi.core.parser.readers.Program

fun convertProgram(program: Program, namespace: GlobalCompilationNamespace): Block {
    val packageDefinition = convertPackageDefinition(program.packageDefinition)
    val moduleName = packageDefinition?.moduleName ?: CompilationDefaults.defaultModule
    val packageName = packageDefinition?.packageName ?: CompilationDefaults.defaultPacakge

    val context = ConversionContext(namespace)
    context.changeCurrentPackage(moduleName, packageName)

    // define imports
    val imports = program.imports.map { convertImportDefinition(context, it) }
    imports.forEach { context.imports.addImport(it) }

    // define package variant types
    registerPackageTypes(context, program.typeDefinitions)
    val typeDefinitions = program.typeDefinitions.map { convertTypeDefinition(context, it) }

    // define package functions and variables
    registerPackageSymbols(context, program)

    val blockBody = mutableListOf<Expression>()
    packageDefinition?.let { blockBody.add(it) }
    blockBody.addAll(imports)
    blockBody.addAll(typeDefinitions)
    blockBody.addAll(program.functions.map { convert(context, it) })
    blockBody.addAll(program.topLevelCode.map { convert(context, it) })
    return Block(blockBody, null)
}

private fun registerPackageTypes(ctx: ConversionContext, defs: List<ParseVariantTypeDefinition>) {
    val variantTypes = defs.map { def ->
        def.typeName to VariantType(
            ctx.currentModule,
            ctx.currentPackage,
            def.typeName,
            def.typeParameters.map { typeParameter -> Type.typeParameter(typeParameter.name) },
            emptyMap(),
            null,
        )
    }.toMap()

    val typesWithVariants = variantTypes.values.zip(defs).map { (baseType, def) ->
        val typeParameters = baseType.genericTypeParameters.map { it.name }.toSet()
        val variants = def.variantConstructors.map {
            VariantType.Variant(
                public = it.public,
                variantName = it.name,
                fields = it.formalFields.map { field ->
                    VariantType.VariantField(
                        field.public,
                        field.name,
                        type = ctx.resolveType(field.typeRef, typeParameters, variantTypes)
                    )
                }
            )
        }

        baseType.variant = variants.singleOrNull()
        baseType to variants
    }

    typesWithVariants.forEach { (type, variants) ->
        ctx.currentPackageDescriptor.typeRegistry.defineVariantType(type, variants)
    }
}

private fun registerPackageSymbols(ctx: ConversionContext, program: Program) {
    program.typeDefinitions.forEach { typeDef ->
        typeDef.variantConstructors.forEach { constructor ->
            val typeParameterNames = typeDef.typeParameters.map { it.name }.toSet()
            val constructorTypeRef = getVariantConstructorTypeRef(typeDef, constructor)
            ctx.currentScope.addSymbol(
                constructor.name,
                ctx.resolveType(constructorTypeRef, typeParameterNames),
                SymbolType.Local,
                public = constructor.public,
                mutable = false
            )
        }
    }

    program.functions.forEach {
        val funcDesc = getFunctionTypeRef(it)
        ctx.currentScope.addSymbol(
            funcDesc.name, ctx.resolveType(funcDesc.type), SymbolType.Local,
            public = when (it) {
                is ParseFuncWithName -> it.public
                is ParseEffectDefinition -> it.public
                else -> TODO("Can't determine if function should be public: $it")
            },
            mutable = false
        )
    }
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
    is ParseInterpolatedString -> convertInterpolatedString(ctx, ast)
    is ParseInterpolation -> convertInterpolation(ctx, ast)
    is StringText -> convertStringText(ast)
    is ParseBinaryOp -> convertBinaryOp(ctx, ast)
    is ParseFnCall -> convertFnCall(ctx, ast)
    is ParseAssignment -> convertAssignment(ctx, ast)
    is ParseIndexedAssignment -> convertIndexedAssignment(ctx, ast)
    is ParseCast -> convertCast(ctx, ast)
    is ParseMethodInvocation -> convertMethodInvocation(ctx, ast)
    is ParseFieldAccess -> convertFieldAccess(ctx, ast)
    is ParseFieldAssignment -> convertFieldAssignment(ctx, ast)
    is ParseGroup -> convertGroup(ctx, ast)
    is ParseIfElse -> convertIfElse(ctx, ast)
    is ParseIndexOperator -> convertIndexOperator(ctx, ast)
    is ParseIs -> convertIs(ctx, ast)
    is ParseNot -> convertNot(ctx, ast)
    is ParseWhen -> convertWhen(ctx, ast)
    is ParseWhile -> convertWhile(ctx, ast)
    is ParseBreak -> convertBreak(ast)
    is ParseContinue -> convertContinue(ast)
    is ParseWeave -> convertWeave(ctx, ast)
    is ParseWeavePlaceholder -> convertWeavePlaceholder(ctx, ast)
    is ParseLambda -> convertLambda(ctx, ast)
    is ParseEffectDefinition -> convertEffectDefinition(ctx, ast)
    is ParseHandle -> convertHandle(ctx, ast)
    else -> TODO("Unsupported conversion of AST element $ast")
}
