import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static util.Utils.evalInt;

public class BitOperationsTest {
    @Test
    public void test_bit_and() {
        assertEquals(1, evalInt("1 & 1"));
        assertEquals(0, evalInt("0 & 1"));
        assertEquals(1, evalInt("3 & 1"));
        assertEquals(0, evalInt("4 & 1"));
    }

    @Test
    public void test_bit_or() {
        assertEquals(1, evalInt("0 | 1"));
        assertEquals(1, evalInt("1 | 1"));
        assertEquals(1, evalInt("1 | 0"));
        assertEquals(0, evalInt("0 | 0"));
    }


    @Test
    public void test_shift_left() {
        assertEquals(8, evalInt("1 << 3"));
    }

    @Test
    public void test_shift_right() {
        assertEquals(1, evalInt("8 >> 3"));
    }
}
