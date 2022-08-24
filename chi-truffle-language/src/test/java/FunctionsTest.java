import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import util.Utils;

public class FunctionsTest {
    @Test
    public void test_simple_lambda_definition() {
        try (var context = Context.create("chi")) {
            var function = context.eval("chi", "fn(): int { 1 }");
            Assert.assertTrue(function.canExecute());
            Assert.assertEquals(1, function.execute().asInt());
        }
    }

    @Test
    public void test_function_execution() {
        var result = Utils.eval("""
                val func = fn(): int { 1 }
                func()
                """).asInt();
        Assert.assertEquals(1, result);
    }

    @Test
    public void test_passing_arguments() {
        var result = Utils.eval("""
                val func = fn(a: int, b: string, c: float): string {
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
                val func = fn(): int { a }
                func()
                """).asInt();
        Assert.assertEquals(10, result);
    }

    @Test
    public void test_nested_function_scoping() {
        var result = Utils.eval("""
                val a = 1
                val outer = fn(): int {
                  val b = 2
                  val inner = fn(): int { a + b }
                  inner()
                }
                outer()
                """).asInt();
        Assert.assertEquals(3, result);
    }

    @Test
    public void test_local_variables_shadowing() {
        var result = Utils.eval("""
                val a = 1
                val func = fn(): int {
                  val a = 2
                  a
                }
                                
                func()
                """).asInt();
        Assert.assertEquals(2, result);
    }

    @Test
    public void test_argument_shadowing_outer_variable() {
        var result = Utils.eval("""
                val a = 1
                val func = fn(a: int): int {
                  a
                }
                                
                func(2)
                """).asInt();
        Assert.assertEquals(2, result);
    }

    @Test
    @Ignore("Not sure if this should even compile as 'a' is already defined")
    public void test_local_variable_shadows_argument() {
        var result = Utils.eval("""
                val func = fn(a: int): int {
                  val a = 1
                  a
                }
                                
                func(2)
                """).asInt();
        Assert.assertEquals(1, result);
    }

    @Test
    public void can_call_arbitrary_expression() {
        var result = Utils.eval("""
                val foo = fn(): () -> int {
                    fn(): int { 42 }
                }
                                
                foo()()
                """).asInt();
        Assert.assertEquals(42, result);
    }

    @Test
    public void test_each_invocation_should_get_own_scope() {
        // TODO
    }
}
