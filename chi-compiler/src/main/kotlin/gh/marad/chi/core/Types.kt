package gh.marad.chi.core

import gh.marad.chi.core.analyzer.typesMatch
import java.util.*

sealed interface Type {
    val name: String
    fun isPrimitive(): Boolean
    fun isNumber(): Boolean

    // can index operator be used
    fun isIndexable(): Boolean = false

    // what type should index be?
    fun expectedIndexType(): Type = undefined

    // what is the type of indexed element
    fun indexedElementType(): Type = undefined

    fun isGenericType(): Boolean
    fun getTypeParameters(): List<Type>
    fun isTypeConstructor(): Boolean = false
    fun construct(concreteTypes: Map<GenericTypeParameter, Type>): Type = this

    fun isCompositeType(): Boolean

    fun toDisplayString(): String = name

    companion object {
        @JvmStatic
        val intType = IntType()

        //        val i64 = I64Type()
        @JvmStatic
        val floatType = FloatType()

        //        val f64 = F64Type()
        @JvmStatic
        val unit = UnitType()

        @JvmStatic
        val bool = BoolType()

        @JvmStatic
        val string = StringType()

        @JvmStatic
        val undefined = UndefinedType()

        @JvmStatic
        val primitiveTypes = listOf(intType, floatType, unit, bool, string)

        @JvmStatic
        val any = AnyType()

        @JvmStatic
        fun fn(returnType: Type, vararg argTypes: Type) =
            FnType(genericTypeParameters = emptyList(), paramTypes = argTypes.toList(), returnType)

        @JvmStatic
        fun genericFn(
            genericTypeParameters: List<GenericTypeParameter>,
            returnType: Type,
            vararg argTypes: Type
        ) = FnType(genericTypeParameters, argTypes.toList(), returnType)

        @JvmStatic
        fun array(elementType: Type) = ArrayType(elementType)

        @JvmStatic
        fun typeParameter(name: String) = GenericTypeParameter(name)
    }
}

data class UndefinedType(override val name: String = "undefined") : Type {
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isGenericType(): Boolean = false
    override fun getTypeParameters(): List<Type> = emptyList()
    override fun isCompositeType(): Boolean = false
    override fun toDisplayString(): String = "<undefined>"
}

sealed interface PrimitiveType : Type {
    override fun isPrimitive(): Boolean = true
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false
    override fun isGenericType(): Boolean = false
    override fun getTypeParameters(): List<Type> = emptyList()
}

sealed interface NumberType : PrimitiveType {
    override fun isPrimitive(): Boolean = true
    override fun isNumber(): Boolean = true
    override fun isCompositeType(): Boolean = false
}

data class IntType internal constructor(override val name: String = "int") : NumberType
data class FloatType internal constructor(override val name: String = "float") : NumberType
data class UnitType internal constructor(override val name: String = "unit") : PrimitiveType
data class BoolType internal constructor(override val name: String = "bool") : PrimitiveType

data class StringType(override val name: String = "string") : Type {
    override fun isPrimitive(): Boolean = true
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false

    override fun isIndexable(): Boolean = true
    override fun expectedIndexType(): Type = Type.intType
    override fun indexedElementType(): Type = Type.string
    override fun isGenericType(): Boolean = false
    override fun getTypeParameters(): List<Type> = emptyList()
}

data class FnType(
    val genericTypeParameters: List<GenericTypeParameter>,
    val paramTypes: List<Type>,
    val returnType: Type
) : GenericType {
    override val name = "(${paramTypes.joinToString(", ") { it.name }}) -> ${returnType.name}"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false
    override fun isGenericType(): Boolean = isTypeConstructor()
    override fun isTypeConstructor(): Boolean =
        paramTypes.any { it.isTypeConstructor() } || returnType.isTypeConstructor()

    override fun construct(concreteTypes: Map<GenericTypeParameter, Type>): Type =
        copy(
            paramTypes = paramTypes.map { it.construct(concreteTypes) },
            returnType = returnType.construct(concreteTypes)
        )

    override fun getTypeParameters(): List<Type> = genericTypeParameters
}

data class OverloadedFnType(val types: Set<FnType>) : Type {
    override val name: String = "overloadedFn"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isGenericType(): Boolean = false
    override fun getTypeParameters(): List<Type> = emptyList()

    override fun isCompositeType(): Boolean = false

    fun addFnType(fnType: FnType) = copy(types = types + fnType)
    fun getType(paramTypes: List<Type>): FnType? =
        findCandidates(paramTypes).singleOrNull()

    private fun findCandidates(actualTypes: List<Type>): List<FnType> {
        val candidates = types.filter {
            actualTypes.size == it.paramTypes.size
                    && it.paramTypes.zip(actualTypes).all { (expected, actual) ->
                typesMatch(expected, actual, acceptAllTypesAsGenericTypeParameter = true)
            }
        }
        val withScores =
            candidates.map { Pair(it, scoreParamTypes(it.paramTypes, actualTypes)) }.sortedByDescending { it.second }
        return if (withScores.isEmpty()) {
            emptyList()
        } else {
            val maxScore = withScores[0].second
            withScores.filter { it.second == maxScore }.map { it.first }
        }
    }

    private fun scoreParamTypes(expectedTypes: List<Type>, actualTypes: List<Type>): Int {
        return expectedTypes.zip(actualTypes).fold(0) { acc, (expected, actual) ->
            when {
                expected == Type.any -> acc
                expected == actual -> acc + 3
                expected.isPrimitive() -> acc + 2
                expected.isCompositeType() -> acc + 1
                else -> acc
            }
        }
    }
}


sealed interface GenericType : Type {
}

data class GenericTypeParameter(val typeParameterName: String) : GenericType {
    override val name: String = typeParameterName

    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isGenericType(): Boolean = true

    override fun isCompositeType(): Boolean = false

    override fun isTypeConstructor(): Boolean = true
    override fun getTypeParameters(): List<Type> = emptyList()
    override fun construct(concreteTypes: Map<GenericTypeParameter, Type>): Type =
        concreteTypes[this] ?: this
}

data class ArrayType(val elementType: Type) : GenericType {
    override val name: String = "array[${elementType.name}]"

    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false
    override fun isIndexable(): Boolean = true
    override fun expectedIndexType(): Type = Type.intType
    override fun indexedElementType(): Type = elementType
    override fun isGenericType(): Boolean = true
    override fun getTypeParameters(): List<Type> = listOf(elementType)
    override fun isTypeConstructor(): Boolean = elementType.isTypeConstructor()
    override fun construct(concreteTypes: Map<GenericTypeParameter, Type>) =
        copy(elementType = elementType.construct(concreteTypes))
}

data class AnyType(override val name: String = "any") : Type {
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isGenericType(): Boolean = false
    override fun getTypeParameters(): List<Type> = emptyList()
    override fun isCompositeType(): Boolean = false
}

sealed interface CompositeType : Type {
    override fun isCompositeType(): Boolean = true
    fun memberType(member: String): Type?
    fun hasMember(member: String): Boolean = false
}

data class VariantType(
    val moduleName: String,
    val packageName: String,
    val simpleName: String,
    val genericTypeParameters: List<GenericTypeParameter>,
    val concreteTypeParameters: Map<GenericTypeParameter, Type>,
    val variant: Variant?
) : CompositeType, GenericType {
    override val name: String = "$moduleName/$packageName.$simpleName"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun toDisplayString(): String =
        "$name[${
            genericTypeParameters.zip(concreteTypeParameters.values)
                .joinToString(", ") { "${it.first.name}=${it.second.name}" }
        }]"

    override fun hasMember(member: String): Boolean = variant?.let {
        variant.fields.any { it.name == member }
    } ?: false

    override fun memberType(member: String): Type? = variant?.let {
        variant.fields.find { it.name == member }?.type
    }

    override fun isGenericType(): Boolean = genericTypeParameters.isNotEmpty()
    override fun getTypeParameters(): List<Type> = genericTypeParameters
    override fun isTypeConstructor(): Boolean =
        isGenericType() // FIXME: tutaj raczej trzeba dla wszystkich parametrów wariantu sprawdzić czy typy pól nie są GenericTypeParameter

    override fun construct(concreteTypes: Map<GenericTypeParameter, Type>): Type =
        copy(
            concreteTypeParameters = concreteTypes,
            variant = variant?.copy(
                fields = variant.fields.map {
                    if (it.type.isGenericType()) {
                        it.copy(type = it.type.construct(concreteTypes))
                    } else {
                        it
                    }
                }
            ))

    data class Variant(val variantName: String, val fields: List<VariantTypeField>)

    override fun hashCode(): Int = Objects.hash(moduleName, packageName, simpleName)
    override fun equals(other: Any?): Boolean =
        other is VariantType
                && other.moduleName == moduleName
                && other.packageName == packageName
                && other.simpleName == simpleName
                && other.concreteTypeParameters.keys.intersect(concreteTypeParameters.keys)
            .all {
                other.concreteTypeParameters[it] == concreteTypeParameters[it]
            }
}