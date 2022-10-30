package gh.marad.chi.core.expressionast.internal

import gh.marad.chi.core.EffectDefinition
import gh.marad.chi.core.Type
import gh.marad.chi.core.parser.readers.ParseEffectDefinition
import gh.marad.chi.core.parser.readers.TypeNameRef
import gh.marad.chi.core.parser.readers.TypeParameterRef
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test


class EffectConversionsKtDefinitionTest {
    @Test
    fun `should be defined in current package`() {
        // given
        val context = defaultContext()

        // when
        val result = convertEffectDefinition(context, sampleEffectDefinition)
            .shouldBeTypeOf<EffectDefinition>()

        // then
        result.moduleName shouldBe context.currentModule
        result.packageName shouldBe context.currentPackage
        result.name shouldBe sampleEffectDefinition.name
    }

    @Test
    fun `type parameters should be resolved in arguments`() {
        // given
        val definition = sampleEffectDefinition.copy(
            typeParameters = listOf(TypeParameterRef("T", sectionA)),
            formalArguments = listOf(arg("t", typeName = "T"))
        )

        // when
        val result = convertEffectDefinition(defaultContext(), definition)
            .shouldBeTypeOf<EffectDefinition>()

        // then
        result.parameters shouldHaveSize 1
        result.parameters.first() should {
            it.name shouldBe "t"
            it.type shouldBe Type.typeParameter("T")
        }
    }

    @Test
    fun `type prameters should be resolved in return type`() {
        // given
        val definition = sampleEffectDefinition.copy(
            typeParameters = listOf(TypeParameterRef("T", sectionA)),
            returnTypeRef = TypeNameRef("T", sectionB)
        )

        // when
        val result = convertEffectDefinition(defaultContext(), definition)
            .shouldBeTypeOf<EffectDefinition>()

        // then
        result.returnType shouldBe Type.typeParameter("T")
    }

    private val sampleEffectDefinition = ParseEffectDefinition(
        public = true,
        name = "effectName",
        typeParameters = emptyList(),
        formalArguments = emptyList(),
        returnTypeRef = intTypeRef,
        testSection
    )
}