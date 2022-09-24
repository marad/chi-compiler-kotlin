import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static util.Utils.prepareContext;

public class StringsTest {
    @Test
    public void simple_string_interpolation() {
        try (var context = prepareContext()) {
            var code = """
                    val input = "world"
                    "hello $input"
                    """;
            var result = context.eval("chi", code);

            assertEquals("hello world", result.asString());
        }
    }

    @Test
    public void should_automatically_cast_other_types() {
        var values = List.of("10", "1.5", "true");
        for (var value : values) {
            try (var context = prepareContext()) {
                var code = """
                        val input = %s
                        "hello $input"
                        """.formatted(value);
                var result = context.eval("chi", code);

                assertEquals("hello %s".formatted(value), result.asString());
            }
        }
    }

    @Test
    public void should_handle_nested_interpolations() {
        try (var context = prepareContext()) {
            var code = """
                    "${ "${ "${ 10 }" }" }"
                    """;
            var result = context.eval("chi", code);

            assertEquals("10", result.asString());
        }
    }
}
