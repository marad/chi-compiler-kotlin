import org.junit.Assert;
import org.junit.Test;

import static util.Utils.eval;
import static util.Utils.prepareContext;

public class ScopeTest {
    @Test
    public void should_declare_new_name_in_scope() {
        var result = eval("""
                val a = 42
                a
                """);
        Assert.assertEquals(42, result.asInt());
    }

    @Test
    public void test_assignment() {
        var result = eval("""
                var x = 0
                x = 42
                x
                """);
        Assert.assertEquals(42, result.asLong());
    }

    @Test
    public void assignment_should_find_variable_in_parent_scope() {
        var result = eval("""
                var x = 0
                var y = 42
                val f = fn() { x = y }
                f()
                x
                """);

        Assert.assertEquals(42, result.asLong());
    }

    @Test
    public void blocks_should_execute_and_return_values() {
        Assert.assertEquals(42, eval("""
                val x = {
                    val a = 42
                    a
                }
                x
                """).asInt());
    }

    @Test
    public void local_args_should_be_preferred() {
        try (var context = prepareContext()) {
            context.eval("chi", """
                    package test/a
                    val x = 10
                    """);

            var result = context.eval("chi", """
                    package test/b
                    import test/a { x }
                    fn func(x: int): int {
                        x
                    }
                    func(42)
                    """);

            Assert.assertEquals(42, result.asInt());
        }
    }
}
