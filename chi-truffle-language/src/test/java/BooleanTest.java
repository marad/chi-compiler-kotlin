import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static util.Utils.evalBoolean;

public class BooleanTest {
    @Test
    public void should_compare_equality() {
        assertTrue(evalBoolean("true == true"));
        assertFalse(evalBoolean("false == true"));
        assertFalse(evalBoolean("true == false"));
        assertTrue(evalBoolean("false == false"));

        assertTrue(evalBoolean("1 == 1"));
        assertFalse(evalBoolean("1 == 2"));

        assertTrue(evalBoolean("1.0 == 1.0"));
        assertFalse(evalBoolean("1.0 == 2.0"));

        assertTrue(evalBoolean("\"hello\" == \"hello\""));
        assertFalse(evalBoolean("\"hello\" == \"world\""));
    }

    @Test
    @Ignore("type system doesn't allow that")
    public void should_compare_equality_for_different_types() {
        assertFalse(evalBoolean("\"hello\" == 42"));
    }

    @Test
    public void should_compare_inequality() {
        assertFalse(evalBoolean("true != true"));
        assertTrue(evalBoolean("false != true"));
        assertTrue(evalBoolean("true != false"));
        assertFalse(evalBoolean("false != false"));

        assertFalse(evalBoolean("1 != 1"));
        assertTrue(evalBoolean("1 != 2"));

        assertFalse(evalBoolean("1.0 != 1.0"));
        assertTrue(evalBoolean("1.0 != 2.0"));

        assertFalse(evalBoolean("\"hello\" != \"hello\""));
        assertTrue(evalBoolean("\"hello\" != \"world\""));
    }

    @Test
    public void should_compare_less_than() {
        assertTrue(evalBoolean("1 < 2"));
        assertFalse(evalBoolean("1 < 1"));
        assertTrue(evalBoolean("1 <= 1"));

        assertTrue(evalBoolean("1.0 < 2.0"));
        assertFalse(evalBoolean("1.0 < 1.0"));
        assertTrue(evalBoolean("1.0 <= 1.0"));
    }

    @Test
    public void should_compare_greater_than() {
        assertTrue(evalBoolean("2 > 1"));
        assertFalse(evalBoolean("1 > 1"));
        assertTrue(evalBoolean("1 >= 1"));

        assertTrue(evalBoolean("2.0 > 1.0"));
        assertFalse(evalBoolean("1.0 > 1.0"));
        assertTrue(evalBoolean("1.0 >= 1.0"));
    }
}
