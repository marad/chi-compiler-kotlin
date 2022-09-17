import org.junit.Assert;
import org.junit.Test;

import static util.Utils.eval;
import static util.Utils.prepareContext;

public class LambdaTest {
    @Test
    public void lambda_should_capture_its_environment() {
        var result = eval("""
                fn foo(f: () -> int): int {
                    f()
                }
                                
                fn bar(): int {
                    val x = 5
                    foo({ 10 + x })
                }
                                
                bar()
                """).asInt();

        Assert.assertEquals(15, result);
    }

    @Test
    public void lambda_should_be_able_to_modify_outer_scope() {
        var result = eval("""
                fn foo(f: () -> unit) {
                    f()
                }
                                
                fn bar(): int {
                    var x = 5
                    foo({ x = 10 })
                    x
                }
                                
                bar()
                """).asInt();

        Assert.assertEquals(10, result);
    }

    @Test
    public void should_enclose_local_and_outer_scope() {
        try (var context = prepareContext()) {
            var code = """
                    var outer = 10
                    fn test(): int {
                        var local = 20
                        val lambda = {
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
                        val lambda = {
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
                        val lambda = {
                            val lambda2 = {
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
