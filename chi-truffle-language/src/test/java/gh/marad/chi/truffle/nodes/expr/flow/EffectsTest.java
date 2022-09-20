package gh.marad.chi.truffle.nodes.expr.flow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static util.Utils.prepareContext;

public class EffectsTest {
    @Test
    public void smoke_test() {
        try (var context = prepareContext()) {
            var code = """
                    effect greet(name: string): string
                                        
                    handle {
                        greet("Andżej")
                    } with {
                        greet(name) -> resume("Hello " + name)
                    }
                    """;
            var result = context.eval("chi", code);

            assertEquals("Hello Andżej", result.asString());
        }
    }
}