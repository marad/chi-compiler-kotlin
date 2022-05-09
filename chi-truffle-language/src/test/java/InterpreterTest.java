import gh.marad.chi.truffle.runtime.Unit;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static util.Utils.eval;
import static util.Utils.evalUnit;

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
        var ex = Assert.assertThrows(PolyglotException.class,
                () -> eval("""
                    { val a = 42 }
                    a
                    """));
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
    @Ignore("I have no idea how to make unit work with this. Possibly some library stuff")
    public void when_else_branch_is_missing_it_returns_unit_value() {
        Assert.assertEquals(Unit.instance, evalUnit("if (false) { 1 }"));
    }
}
