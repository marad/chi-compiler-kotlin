import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import gh.marad.chi.truffle.runtime.Unit;
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
    public void fibonacci_test() {
        Assert.assertEquals(832040, eval("""
                val fib = fn(n: int): int {
                    if (n == 0) { 0 }
                    else if (n == 1) { 1 }
                    else { fib(n - 1) + fib(n - 2) }
                }
                
                fib(20)
                """).asInt());
    }

    @Test
    public void man_or_boy_test() {
        // https://en.wikipedia.org/wiki/Man_or_boy_test
        var result = eval("""
                val a = fn(k: int, x1: () -> int, x2: () -> int, x3: () -> int, x4: () -> int, x5: () -> int): int {
                  val b = fn(): int {
                    k = k - 1
                    a(k, b, x1, x2, x3, x4)
                  }
                  
                  if (k <= 0) {
                    x4() + x5()
                  } else {
                    b()
                  }
                }
                
                a(10, fn(): int { 1 }, fn(): int { 0-1 }, fn(): int { 0-1 }, fn(): int { 1 }, fn(): int { 0 })
                """);

        Assert.assertEquals(-67, result.asLong());
    }

    @Test
    public void foo() {
        var builder = FrameDescriptor.newBuilder();
        builder.addSlot(FrameSlotKind.Int, "a", null);
        builder.addSlot(FrameSlotKind.Boolean, "qwe", null);

        var frameDesc = builder.build();

        System.out.println(frameDesc.getNumberOfSlots());
        for (int i = 0; i < frameDesc.getNumberOfSlots(); i++) {
            var slotName = frameDesc.getSlotName(i);
            System.out.println("Slot name %s".formatted(slotName));
        }
        System.out.println(frameDesc.getAuxiliarySlots());
    }
}
