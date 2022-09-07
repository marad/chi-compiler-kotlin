package gh.marad.chi.core.astconverter.internal

import gh.marad.chi.core.*
import gh.marad.chi.core.astconverter.ConversionContext
import gh.marad.chi.core.astconverter.convert
import gh.marad.chi.core.namespace.SymbolScope
import gh.marad.chi.core.parser.readers.*

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

fun convertFnCall(ctx: ConversionContext, ast: ParseFnCall): Expression {
    return FnCall(
        enclosingScope = ctx.currentScope,
        name = ast.name, // FIXME: wydaje mi się, że to `name` nie jest potrzebne - zweryfikować w truffle
        function = convert(ctx, ast.function),
        callTypeParameters = ast.concreteTypeParameters.map { ctx.resolveType(it) },
        parameters = ast.arguments.map { convert(ctx, it) },
        location = ast.section.asLocation()
    )
}

data class FunctionDescriptorWithTypeRef(val name: String, val type: TypeRef)

fun getFunctionTypeRef(it: ParseAst): FunctionDescriptorWithTypeRef {
    return when (it) {
        is ParseNameDeclaration -> {
            val func = it.value as ParseFunc
            val funcTypeRef = FunctionTypeRef(
                typeParameters = emptyList(),
                argumentTypeRefs = func.formalArguments.map { it.typeRef },
                func.returnTypeRef,
                null
            )
            val typeRef = it.typeRef ?: funcTypeRef
            FunctionDescriptorWithTypeRef(it.name.name, typeRef)
        }

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

