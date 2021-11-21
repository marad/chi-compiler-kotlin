package gh.marad.chi.core

sealed interface Type {
    val name: String

    companion object {
        val i32 = SimpleType("i32")
        val f32 = SimpleType("f32")
        val unit = SimpleType("unit")
        val bool = SimpleType("bool")
        fun fn(returnType: Type, vararg argTypes: Type) =
            FnType(paramTypes = argTypes.toList(), returnType)
    }
}

data class SimpleType(override val name: String) : Type

data class FnType(val paramTypes: List<Type>, val returnType: Type) : Type {
    override val name = "(${paramTypes.joinToString(", ") { it.name }}) -> ${returnType.name}"
}