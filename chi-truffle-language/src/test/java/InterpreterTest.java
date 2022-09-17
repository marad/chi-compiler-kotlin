import org.junit.Assert;
import org.junit.Test;

import static util.Utils.eval;

public class InterpreterTest {
    @Test
    public void evaluating_simple_values() {
        Assert.assertEquals(10, eval("10").asInt());
        Assert.assertEquals(21474836470L, eval("21474836470").asLong());
        Assert.assertEquals(10.5f, eval("10.5").asFloat(), 0.1f);
        Assert.assertEquals("hello", eval("\"hello\"").asString());
        Assert.assertTrue(eval("true").asBoolean());
        Assert.assertFalse(eval("false").asBoolean());
    }

    @Test
    public void test_casting() {
        Assert.assertEquals(42L, eval("42.0 as int").asLong());
        Assert.assertEquals(42f, eval("42 as float").asFloat(), 0.1f);
        Assert.assertEquals("42", eval("42 as string").asString());
    }

    @Test
    public void if_else_should_evaluate_appropriate_branch() {
        Assert.assertEquals(1, eval("if (true) { 1 } else { 2 }").asInt());
        Assert.assertEquals(2, eval("if (false) { 1 } else { 2 }").asInt());
    }

    @Test
    public void else_branch_can_be_omitted() {
        Assert.assertEquals(1, eval("if (true) { 1 }").asInt());
    }

    @Test
    public void test_while_loop() {
        var result = eval("""
                var i = 4
                var sum = 2
                while(i > 0) {
                    i = i - 1
                    sum = sum + 10
                }
                sum
                """);

        Assert.assertEquals(42, result.asLong());
    }

    @Test
    public void test_break_within_while() {
        var result = eval("""
                var i = 0
                while(i < 10) {
                    i = i + 1
                    if (i >= 4) {
                        break
                    }
                }
                i
                """);

        Assert.assertEquals(4, result.asInt());
    }

    @Test
    public void test_function_sugar() {
        var result = eval("""
                fn foo(a: int): int {
                  a
                }
                foo(10)
                """);
        Assert.assertEquals(10, result.asLong());
    }

    @Test
    public void fibonacci_test() {
        Assert.assertEquals(6765, eval("""
                fn fib(n: int): int {
                    if (n == 0) { 0 }
                    else if (n == 1) { 1 }
                    else { fib(n - 1) + fib(n - 2) }
                }
                                
                fib(20)
                """).asInt());
    }

    @Test
    public void recursion_test() {
        eval("""
                fn a(): int { 1 }
                fn fun(n: int): int {
                    a()
                }
                                
                fun(0)
                """);
    }
}
