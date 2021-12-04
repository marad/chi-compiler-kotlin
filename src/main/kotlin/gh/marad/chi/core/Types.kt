package gh.marad.chi.core

sealed interface Type {
    val name: String
    fun isPrimitive(): Boolean
    fun isNumber(): Boolean

    companion object {
        val i32 = I32Type()
        val i64 = I64Type()
        val f32 = F32Type()
        val f64 = F64Type()
        val unit = UnitType()
        val bool = BoolType()
        val undefined = UndefinedType()

        val primitiveTypes = listOf(i32, i64, f32, f64, unit, bool)

        fun fn(returnType: Type, vararg argTypes: Type) =
            FnType(paramTypes = argTypes.toList(), returnType)
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

data class I32Type internal constructor(override val name: String = "i32") : NumberType
data class I64Type internal constructor(override val name: String = "i64") : NumberType
data class F32Type internal constructor(override val name: String = "f32") : NumberType
data class F64Type internal constructor(override val name: String = "f64") : NumberType
data class UnitType internal constructor(override val name: String = "unit") : PrimitiveType
data class BoolType internal constructor(override val name: String = "bool") : PrimitiveType

data class FnType(val paramTypes: List<Type>, val returnType: Type) : Type {
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