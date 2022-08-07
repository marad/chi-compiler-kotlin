package gh.marad.chi.core

sealed interface Type {
    val name: String
    fun isPrimitive(): Boolean
    fun isNumber(): Boolean

    // can index operator be used
    fun isIndexable(): Boolean = false

    // what type should index be?
    fun expectedIndexType(): Type? = null

    // what is the type of indexed element
    fun indexedElementType(): Type? = null

    fun isGenericType(): Boolean = false

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
}

sealed interface PrimitiveType : Type {
    override fun isPrimitive(): Boolean = true
    override fun isNumber(): Boolean = false
}

sealed interface NumberType : Type {
    override fun isPrimitive(): Boolean = true
    override fun isNumber(): Boolean = true
}

data class IntType internal constructor(override val name: String = "int") : NumberType
data class FloatType internal constructor(override val name: String = "float") : NumberType
data class UnitType internal constructor(override val name: String = "unit") : PrimitiveType
data class BoolType internal constructor(override val name: String = "bool") : PrimitiveType

data class StringType(override val name: String = "string") : Type {
    override fun isPrimitive(): Boolean = true
    override fun isNumber(): Boolean = false
}

data class FnType(
    val genericTypeParameters: List<GenericTypeParameter>,
    val paramTypes: List<Type>,
    val returnType: Type
) : Type {
    override val name = "(${paramTypes.joinToString(", ") { it.name }}) -> ${returnType.name}"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
}

data class OverloadedFnType(val types: Set<FnType>) : Type {
    override val name: String = "overloadedFn"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false

    fun addFnType(fnType: FnType) = copy(types = types + fnType)
    fun getType(paramTypes: List<Type>): FnType? =
        types.find { it.paramTypes == paramTypes }
}


data class GenericTypeParameter(val typeParameterName: String) : Type {
    override val name: String = "genericTypeParameter"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false

}

sealed interface GenericType : Type {
    override fun isGenericType(): Boolean = true

    fun typeParameterCount(): Int
    fun construct(concreteTypes: List<Type>): Type
}

data class ArrayType(val elementType: Type) : GenericType {
    override val name: String = "array"

    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isIndexable(): Boolean = true
    override fun expectedIndexType(): Type = Type.intType
    override fun indexedElementType(): Type = elementType

    override fun typeParameterCount(): Int = 1
    override fun construct(concreteTypes: List<Type>) = copy(elementType = concreteTypes[0])
}
