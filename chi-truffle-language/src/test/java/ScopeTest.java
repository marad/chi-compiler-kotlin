import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static util.Utils.eval;

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
    @Ignore("This is awkward to do on this level")
    // Maybe I could use interop bindings for this. We'll see.
    // For now if I wanted a REPL I could just implement it in Chi itself, then top frame would naturally be shared
    public void declared_names_should_persist_across_evaluations() {
        try(var context = Context.create("chi")) {
            context.eval("chi", "val a = 42");
            Assert.assertEquals(42, context.eval("chi", "a").asInt());
        }
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
}
