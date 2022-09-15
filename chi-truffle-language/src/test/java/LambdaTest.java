import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class LambdaTest {
    @Test
    public void should_enclose_local_and_outer_scope() {
        try (var context = prepareContext()) {
            var code = """
                    var outer = 10
                    fn test(): int {
                        var local = 20
                        val lambda = fn(): int {
                            outer = outer + 1
                            local + 1
                        }
                        lambda()
                    }
                    test()
                    """;
            var result = context.eval("chi", code);

            Assert.assertEquals(11, context.eval("chi", "outer").asInt());
            Assert.assertEquals(21, result.asInt());
        }
    }

    @Test
    public void should_enclose_arguments() {
        try (var context = prepareContext()) {
            var code = """
                    fn test(arg: int): int {
                        val lambda = fn(): int {
                            arg + 1
                        }
                        lambda()
                    }
                    test(10)
                    """;
            var result = context.eval("chi", code);

            Assert.assertEquals(11, result.asInt());
        }
    }

    @Test
    public void should_enclose_arguments_from_multiple_levels() {
        try (var context = prepareContext()) {
            var code = """
                    fn test(arg: int): int {
                        val lambda = fn(): int {
                            val lambda2 = fn(): int {
                              arg + 1
                            }
                            lambda2()
                        }
                        lambda()
                    }
                    test(10)
                    """;
            var result = context.eval("chi", code);

            Assert.assertEquals(11, result.asInt());
        }
    }
}
