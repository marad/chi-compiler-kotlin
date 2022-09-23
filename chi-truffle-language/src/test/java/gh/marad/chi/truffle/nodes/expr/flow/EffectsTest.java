package gh.marad.chi.truffle.nodes.expr.flow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static util.Utils.prepareContext;

public class EffectsTest {
    @Test
    public void resuming_simple_value() {
        try (var context = prepareContext()) {
            var code = """
                    effect greet(name: string): string
                                        
                    val greetMessage = "Hello "
                                        
                    handle {
                        greet("World")
                    } with {
                        greet(name) -> {
                            resume(greetMessage + name)
                            "Other!"
                        }
                    }
                    """;
            var result = context.eval("chi", code);

            assertEquals("Hello World", result.asString());
        }
    }

    @Test
    public void aborting_an_effect() {
        try (var context = prepareContext()) {
            var code = """
                    effect addOne(num: int): int
                                        
                    handle {
                        val result = addOne(10)
                        "Result: " + result as string
                    } with {
                        addOne(value) -> "I don't feel like it!"
                    }
                    """;

            var result = context.eval("chi", code);

            assertEquals("I don't feel like it!", result.asString());
        }
    }

    @Test
    public void nested_effect_invocation() {
        try (var context = prepareContext()) {
            var code = """
                    effect addOne(num: int): int
                                        
                    fn bar(): int { addOne(10) }
                    fn foo(): int { bar() }
                                        
                    handle {
                        val result = foo()
                        "Result: " + result as string
                    } with {
                        addOne(value) -> resume(value + 1)
                    }
                    """;

            var result = context.eval("chi", code);

            assertEquals("Result: 11", result.asString());
        }
    }

    @Test
    public void overriding_effects() {
        try (var context = prepareContext()) {
            var code = """
                    effect addOne(num: int): int
                                       
                    handle {
                        val value = handle {
                            addOne(0)
                        } with {
                            addOne(value) -> resume(value + 2)
                        }
                        addOne(value)
                    } with {
                        addOne(value) -> resume(value + 1)
                    }
                    """;

            var result = context.eval("chi", code);

            assertEquals(3, result.asInt());
        }
    }
}