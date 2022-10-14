package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.Type
import gh.marad.chi.core.parser.readers.FormalField
import gh.marad.chi.core.parser.readers.ParseVariantTypeDefinition
import gh.marad.chi.core.parser.readers.TypeNameRef
import gh.marad.chi.core.parser.readers.TypeParameter
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/*

 */

class VariantTypeConversionsKtTypeDefinitionTest {
    @Test
    fun `type parameters should be resolved in constructor arguments`() {
        // given
        val context = defaultContext()
        context.addTypeDefinition(sampleDefinition.typeName, listOf(sampleConstructor.name))

        val definition = sampleDefinition.copy(
            typeParameters = listOf(TypeParameter("T", sectionA)),
            variantConstructors = listOf(
                sampleConstructor.copy(
                    formalFields = listOf(
                        FormalField(
                            public = true,
                            name = "a",
                            typeRef = TypeNameRef("T", sectionB),
                            sectionC
                        )
                    )
                )
            )
        )

        // when
        val ctors = convertTypeDefinition(context, definition)
            .constructors

        // then
        ctors shouldHaveSize 1
        val fields = ctors.first().fields
        fields shouldHaveSize 1
        fields.first() should {
            it.name shouldBe "a"
            it.type shouldBe Type.typeParameter("T")
            it.public.shouldBeTrue()
            it.sourceSection shouldBe sectionC
        }
    }

    private val sampleConstructor = ParseVariantTypeDefinition.Constructor(
        public = true,
        name = "Constructor",
        formalFields = emptyList(),
        sectionA
    )

    private val sampleDefinition = ParseVariantTypeDefinition(
        typeName = "TypeName",
        typeParameters = emptyList(),
        variantConstructors = listOf(sampleConstructor),
        sectionB
    )
}