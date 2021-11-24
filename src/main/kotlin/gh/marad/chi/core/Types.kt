package gh.marad.chi.core

sealed interface Type {
    val name: String

    companion object {
        val i32 = I32Type()
        val i64 = I64Type()
        val f32 = F32Type()
        val f64 = F64Type()
        val unit = UnitType()
        val bool = BoolType()

        val primitiveTypes = listOf(i32, i64, f32, f64, unit, bool)

        fun fn(returnType: Type, vararg argTypes: Type) =
            FnType(paramTypes = argTypes.toList(), returnType)
    }
}

sealed interface PrimitiveType : Type

class I32Type internal constructor() : PrimitiveType { override val name: String = "i32" }
class I64Type internal constructor() : PrimitiveType { override val name: String = "i64" }
class F32Type internal constructor() : PrimitiveType { override val name: String = "f32" }
class F64Type internal constructor() : PrimitiveType { override val name: String = "f64" }
class UnitType internal constructor() : PrimitiveType { override val name: String = "unit" }
class BoolType internal constructor() : PrimitiveType { override val name: String = "bool" }

data class FnType(val paramTypes: List<Type>, val returnType: Type) : Type {
    override val name = "(${paramTypes.joinToString(", ") { it.name }}) -> ${returnType.name}"
}