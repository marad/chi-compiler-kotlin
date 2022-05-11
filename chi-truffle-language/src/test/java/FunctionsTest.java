import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Test;
import util.Utils;

public class FunctionsTest {
    @Test
    public void test_simple_lambda_definition() {
        try(var context = Context.create("chi")) {
            var function = context.eval("chi", "fn() { 1 }");
            Assert.assertTrue(function.canExecute());
            Assert.assertEquals(1, function.execute().asInt());
        }
    }

    @Test
    public void test_function_execution() {
        var result = Utils.eval("""
                val func = fn() { 1 }
                func()
                """).asInt();
        Assert.assertEquals(1, result);
    }

    @Test
    public void test_passing_arguments() {
        var result = Utils.eval("""
                val func = fn(a: int, b: string, c: float) {
                    b + (a as string) + (c as string)
                }
                func(10, "hello", 4.2)
                """).asString();
        Assert.assertEquals("hello104.2", result);
    }

    @Test
    public void test_function_should_see_external_scope() {
        var result = Utils.eval("""
                val a = 10
                val func = fn() { a }
                func()
                """).asInt();
        Assert.assertEquals(10, result);
    }
}
