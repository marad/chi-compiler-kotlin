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
    fun isTypeConstructor(): Boolean = false
    fun construct(concreteTypes: Map<GenericTypeParameter, Type>): Type {
        TODO("Add 'This is not a type constructor' exception")
    }

    fun isCompositeType(): Boolean

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
    override fun isCompositeType(): Boolean = false
}

sealed interface PrimitiveType : Type {
    override fun isPrimitive(): Boolean = true
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false
}

sealed interface NumberType : Type {
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
}

data class FnType(
    val genericTypeParameters: List<GenericTypeParameter>,
    val paramTypes: List<Type>,
    val returnType: Type
) : Type {
    override val name = "(${paramTypes.joinToString(", ") { it.name }}) -> ${returnType.name}"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false
}

data class OverloadedFnType(val types: Set<FnType>) : Type {
    override val name: String = "overloadedFn"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false

    fun addFnType(fnType: FnType) = copy(types = types + fnType)
    fun getType(paramTypes: List<Type>): FnType? =
        types.find { it.paramTypes == paramTypes }
}


sealed interface GenericType : Type {
    override fun isGenericType(): Boolean = true
    fun getTypeParameters(): List<Type>
}

data class GenericTypeParameter(val typeParameterName: String) : GenericType {
    override val name: String = typeParameterName

    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false

    override fun isTypeConstructor(): Boolean = true
    override fun getTypeParameters(): List<Type> = emptyList()
    override fun construct(concreteTypes: Map<GenericTypeParameter, Type>): Type =
        concreteTypes[this] ?: TODO("Couldn't find type parameter")
}

data class ArrayType(val elementType: Type) : GenericType {
    override val name: String = "array[${elementType.name}]"

    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun isCompositeType(): Boolean = false
    override fun isIndexable(): Boolean = true
    override fun expectedIndexType(): Type = Type.intType
    override fun indexedElementType(): Type = elementType

    override fun getTypeParameters(): List<Type> = listOf(elementType)
    override fun isTypeConstructor(): Boolean = elementType.isTypeConstructor()
    override fun construct(concreteTypes: Map<GenericTypeParameter, Type>) =
        copy(elementType = (elementType as GenericType).construct(concreteTypes))
}

sealed interface CompositeType : Type {
    override fun isCompositeType(): Boolean = true
    fun memberType(member: String): Type?
    fun hasMember(member: String): Boolean = false
}

data class ComplexType(
    val moduleName: String,
    val packageName: String,
    val simpleName: String,
    val genericTypeParameters: List<GenericTypeParameter>,
) : CompositeType {
    override val name: String = "$moduleName/$packageName.$simpleName"

    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun memberType(member: String): Type? {
        TODO("This should not be invoked ever")
    }

//    override fun isGenericType(): Boolean = false
//    override fun isTypeConstructor(): Boolean = false
//    override fun construct(concreteTypes: Map<GenericTypeParameter, Type>): Type = TODO()

}

data class ComplexTypeVariant(
    val moduleName: String,
    val packageName: String,
    val simpleName: String,
    val baseType: Type,
    val fields: List<ComplexTypeField>,
) : CompositeType {
    override val name: String = "$moduleName/$packageName.$simpleName"
    override fun isPrimitive(): Boolean = false
    override fun isNumber(): Boolean = false
    override fun memberType(member: String): Type? {
        return fields.find { it.name == member }?.type
    }

    override fun hasMember(member: String): Boolean = fields.any { it.name == member }
}
