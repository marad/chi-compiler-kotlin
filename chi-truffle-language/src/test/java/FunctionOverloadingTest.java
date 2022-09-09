import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class FunctionOverloadingTest {
    @Test
    public void should_find_proper_function() {
        try (var context = prepareContext()) {
            // given
            context.eval("chi", """
                    fn f(i: int): int { 0 }
                    fn f(s: string): int { 1 }
                    fn f[T](t: T): int { 2 }
                    """);

            // expect
            Assert.assertEquals(0, context.eval("chi", "f(0)").asInt());
            Assert.assertEquals(1, context.eval("chi", "f(\"hello\")").asInt());
            Assert.assertEquals(2, context.eval("chi", "f(2.2)").asInt());
        }
    }

    @Test
    public void foo_bar() {
        try (var context = prepareContext()) {
            var result = context.eval("chi", """
                    fn foo(i: int) {}
                    fn foo(i: int): int { i }
                    foo(10)
                    """);

            Assert.assertEquals(10, result.asInt());
        }
    }
}
