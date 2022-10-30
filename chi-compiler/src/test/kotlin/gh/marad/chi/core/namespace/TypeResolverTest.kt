package gh.marad.chi.core.namespace

import gh.marad.chi.core.GenericTypeParameter
import gh.marad.chi.core.Type
import gh.marad.chi.core.VariantType
import gh.marad.chi.core.expressionast.internal.sectionA
import gh.marad.chi.core.expressionast.internal.sectionB
import gh.marad.chi.core.expressionast.internal.testSection
import gh.marad.chi.core.parser.readers.*
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class TypeResolverTest {
    val resolver = TypeResolver()

    @Test
    fun `should resolve type ref by name`() {
        val type = resolveRef(ref = nameRef("int"), getTypeByName = basicTypeMappings)
        type shouldBe Type.intType
    }

    @Test
    fun `should resolve type parameter by name`() {
        val type = resolveRef(ref = nameRef("T"), typeParameters = setOf("T"))
        type shouldBe Type.typeParameter("T")
    }

    @Test
    fun `should resolve function type ref`() {
        // given
        val functionTypeRef = FunctionTypeRef(
            typeParameters = emptyList(),
            argumentTypeRefs = listOf(nameRef("int"), nameRef("bool")),
            returnType = nameRef("string"), testSection
        )

        // when
        val type = resolveRef(functionTypeRef)

        // then
        type shouldBe Type.fn(Type.string, Type.intType, Type.bool)
    }

    @Test
    fun `should resolve type constructor ref`() {
        // given
        val typeConstructorRef = TypeConstructorRef(
            baseType = nameRef("array"),
            typeParameters = listOf(nameRef("int")),
            testSection
        )

        // when
        val type = resolveRef(typeConstructorRef)

        // then
        type shouldBe Type.array(Type.intType)
    }

    @Test
    fun `should resolve variant type by variant name ref`() {
        // given
        val variantNameRef = VariantNameRef(
            variantType = nameRef("Option"),
            variantName = "Just",
            variantFields = listOf(
                FormalField(
                    public = true,
                    name = "value",
                    typeRef = nameRef("T"),
                    sectionA
                )
            ),
            sectionB
        )

        // when
        val type = resolveRef(
            variantNameRef,
            getTypeByName = nameToTypeMapping("Option" to optionType),
            getVariants = ::getOptionVariants,
        )

        // then
        type shouldBe optionType.withVariant(justVariant)
    }

    @Test
    fun `should respect type parameters from context`() {
        // when
        val type = resolver.withTypeParameters(setOf("X")) {
            resolveRef(nameRef("X"))
        }

        // then
        type shouldBe Type.typeParameter("X")
    }

    private val basicTypeMappings = nameToTypeMapping(
        "int" to Type.intType,
        "float" to Type.floatType,
        "string" to Type.string,
        "bool" to Type.bool,
        "array" to Type.array(Type.typeParameter("T"))
    )

    private fun resolveRef(
        ref: TypeRef, typeParameters: Set<String> = emptySet(), getTypeByName: (String) -> Type = basicTypeMappings,
        getVariants: (VariantType) -> List<VariantType.Variant> = { emptyList() }
    ): Type = resolver.resolve(ref, typeParameters, getTypeByName, getVariants)

    private fun nameRef(name: String): TypeRef = TypeNameRef(name, testSection)

    private fun nameToTypeMapping(vararg args: Pair<String, Type>): (String) -> Type {
        val map = args.toMap()
        return { typeName: String ->
            assert(map.containsKey(typeName)) { "You didn't provide mapping for type $typeName!" }
            map[typeName]!!
        }
    }

    private val optionType = VariantType(
        moduleName = "std",
        packageName = "lang",
        simpleName = "Option",
        genericTypeParameters = listOf(GenericTypeParameter("T")),
        concreteTypeParameters = emptyMap(),
        variant = null
    )

    private val justVariant = VariantType.Variant(
        public = true,
        variantName = "Just",
        fields = listOf(VariantType.VariantField(public = true, name = "value", type = Type.typeParameter("T")))
    )

    private val noneVariant = VariantType.Variant(
        public = true,
        variantName = "None",
        fields = emptyList()
    )

    private fun getOptionVariants(variantType: VariantType): List<VariantType.Variant> {
        if (variantType == optionType) {
            return listOf(noneVariant, justVariant)
        } else {
            throw RuntimeException("This only for use with sample option variant!")
        }
    }
}
