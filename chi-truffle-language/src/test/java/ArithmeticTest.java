import org.junit.Assert;
import org.junit.Test;

import static util.Utils.eval;

public class ArithmeticTest {
    @Test
    public void should_add_two_numbers() {
        Assert.assertEquals(42, eval("30 + 12").asInt());
        Assert.assertEquals(42f, eval("30.0 + 12.0").asFloat(), 0.1f);
    }

    @Test
    public void should_concatenate_strings() {
        Assert.assertEquals("hello world", eval("\"hello \" + \"world\"").asString());
    }

    @Test
    public void should_subtract_two_numbers() {
        Assert.assertEquals(42, eval("50 - 8").asInt());
        Assert.assertEquals(42f, eval("50.0 - 8.0").asFloat(), 0.1f);
    }

    @Test
    public void should_multiply_two_numbers() {
        Assert.assertEquals(42, eval("21 * 2").asInt());
        Assert.assertEquals(42f, eval("21.0 * 2.0").asFloat(), 0.1f);
    }

    @Test
    public void should_divide_two_numbers() {
        Assert.assertEquals(42, eval("84 / 2").asInt());
        Assert.assertEquals(42f, eval("84.0 / 2.0").asFloat(), 0.1f);
    }

    @Test
    public void should_calc_modulo_of_two_numbers() {
        Assert.assertEquals(42, eval("142 % 100").asInt());
    }
}
