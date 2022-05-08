import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

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
    public void should_declare_new_name_in_scope() {
        var result = eval("""
                val a = 42
                a
                """);
        Assert.assertEquals(42, result.asInt());
    }

    @Test
    public void declared_names_should_persist_across_evaluations() {
        try(var context = Context.create("chi")) {
            context.eval("chi", "val a = 42");
            Assert.assertEquals(42, context.eval("chi", "a").asInt());
        }
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
    public void variables_defined_inside_blocks_should_not_be_visible_outside() {
        var ex = Assert.assertThrows(PolyglotException.class, () -> {
            eval("""
                { val a = 42 }
                a
                """);
        });
    }

    private Value eval(String code) {
        try(var context = Context.create("chi")) {
            return context.eval("chi", code);
        }
    }
}
