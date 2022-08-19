import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class GenericCompositeTypesTest {
    @Test
    public void create_generic_type() {
        try (var context = prepareContext()) {
            // when
            var result = context.eval("chi", """
                    data Maybe[T] = Just(t: T) | Nothing
                    Just(5)
                    """);

            // then
            Assert.assertEquals(5, result.getMember("t").asInt());
        }
    }

    @Test
    public void use_only_some_generic_parameters_in_variant_constructors() {
        try (var context = prepareContext()) {
            // given
            context.eval("chi", "data Result[V, E] = Ok(value: V) | Err(error: E)");
            // when
            var ok = context.eval("chi", """
                    val ok: Result[int, string] = Ok(5)
                    """);
            var err = context.eval("chi", """
                    val err: Result[int, string] = Err("error")
                    """);

            // then
            Assert.assertEquals(5, ok.getMember("value").asInt());
            Assert.assertEquals("error", err.getMember("error").asString());
        }
    }


    @Test
    public void foo() {
        try (var context = prepareContext()) {
            context.eval("chi", "data Result[V, E] = Ok(value: V) | Err(error: E)");
            context.eval("chi", """
                    val ok: Result[int, string] = Ok("hello")
                    """);
        }
    }
}
