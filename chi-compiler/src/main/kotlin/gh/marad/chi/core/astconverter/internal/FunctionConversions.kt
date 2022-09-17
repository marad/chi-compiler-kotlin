package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.namespace.SymbolType
import gh.marad.chi.core.parser.readers.*

fun convertLambda(ctx: ConversionContext, ast: ParseLambda): Expression {
    return ctx.withNewFunctionScope {
        val params = ast.formalArguments.map {
            FnParam(
                it.name,
                ctx.resolveType(it.typeRef),
                it.section
            ).also { param ->
                ctx.currentScope.addSymbol(param.name, param.type, SymbolType.Argument, mutable = false)
            }
        }
        val body = ast.body.map { convert(ctx, it) }
        Fn(
            fnScope = ctx.currentScope,
            genericTypeParameters = emptyList(),
            parameters = params,
            returnType = body.lastOrNull()?.type ?: Type.unit,
            body = Block(body, ast.section),
            sourceSection = ast.section,
        )
    }
}

fun convertFunc(ctx: ConversionContext, ast: ParseFunc): Expression =
    ctx.withNewFunctionScope {
        Fn(
            fnScope = ctx.currentScope,
            genericTypeParameters = emptyList(),
            parameters = ast.formalArguments.map {
                FnParam(
                    it.name,
                    ctx.resolveType(it.typeRef),
                    it.section
                ).also { param ->
                    ctx.currentScope.addSymbol(param.name, param.type, SymbolType.Argument, mutable = false)
                }
            },
            returnType = ast.returnTypeRef.let { ctx.resolveType(it) },
            body = convert(ctx, ast.body) as Block,
            sourceSection = ast.section
        )
    }

fun convertFuncWithName(ctx: ConversionContext, ast: ParseFuncWithName): Expression {
    val typeParameterNames = ast.typeParameters.map { it.name }.toSet()
    return NameDeclaration(
        enclosingScope = ctx.currentScope,
        name = ast.name,
        value = ctx.withNewFunctionScope {
            Fn(
                fnScope = ctx.currentScope,
                genericTypeParameters = ast.typeParameters.map { GenericTypeParameter(it.name) },
                parameters = ast.formalArguments.map {
                    FnParam(
                        it.name,
                        ctx.resolveType(it.typeRef, typeParameterNames),
                        it.section
                    ).also { param ->
                        ctx.currentScope.addSymbol(param.name, param.type, SymbolType.Argument, mutable = false)
                    }
                },
                returnType = ast.returnTypeRef?.let { ctx.resolveType(it, typeParameterNames) } ?: Type.unit,
                body = ctx.withTypeParameters(typeParameterNames) { convert(ctx, ast.body) as Block },
                sourceSection = ast.section
            )
        },
        mutable = false,
        expectedType = null,
        sourceSection = ast.section
    )
}

fun convertFnCall(ctx: ConversionContext, ast: ParseFnCall): Expression {
    return FnCall(
        function = convert(ctx, ast.function),
        callTypeParameters = ast.concreteTypeParameters.map { ctx.resolveType(it) },
        parameters = ast.arguments.map { convert(ctx, it) },
        sourceSection = ast.section
    )
}

data class FunctionDescriptorWithTypeRef(val name: String, val type: TypeRef)

fun getFunctionTypeRef(it: ParseAst): FunctionDescriptorWithTypeRef {
    return when (it) {
//        is ParseNameDeclaration -> {
//            val func = it.value as ParseLambda
//            val funcTypeRef = FunctionTypeRef(
//                typeParameters = emptyList(),
//                argumentTypeRefs = func.formalArguments.map { it.typeRef },
//                func.returnTypeRef,
//                null
//            )
//            val typeRef = it.typeRef ?: funcTypeRef
//            FunctionDescriptorWithTypeRef(it.name.name, typeRef)
//        }

        is ParseFuncWithName -> {
            val argumentTypeRefs = it.formalArguments.map { it.typeRef }
            val functionTypeRef = FunctionTypeRef(
                it.typeParameters,
                argumentTypeRefs,
                it.returnTypeRef ?: TypeNameRef("unit", null),
                null
            )
            val typeRef = if (it.typeParameters.isEmpty()) {
                functionTypeRef
            } else {
                TypeConstructorRef(functionTypeRef, it.typeParameters, null)
            }
            FunctionDescriptorWithTypeRef(it.name, typeRef)
        }

        else -> TODO("This is not a function declaration: $it")
    }
}

