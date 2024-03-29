import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class WhenTest {
    @Test
    public void smoke_test() {
        try (var context = prepareContext()) {
            context.eval("chi", "data Option[T] = Just(value: T) | Nothing");

            var result = context.eval("chi", """
                    val x = Just(5)
                    when {
                        x is Just -> x.value
                        x is Nothing -> 0
                        else -> 100
                    }
                    """);

            Assert.assertEquals(5, result.asInt());
        }
    }

    @Test
    public void should_evaluate_only_the_body_of_the_first_condition() {
        try (var context = prepareContext()) {
            var result = context.eval("chi", """
                    when {
                        true -> 1
                        true -> 2
                        true -> 3
                        else -> 0
                    }
                    """);
            Assert.assertEquals(1, result.asInt());
        }
    }

    @Test
    public void else_should_be_evaluated_if_other_branches_fail() {
        try (var context = prepareContext()) {
            var result = context.eval("chi", """
                    when {
                        false -> 1
                        false -> 2
                        else -> 0
                    }
                    """);
            Assert.assertEquals(0, result.asInt());
        }
    }

    @Test
    public void is_variant_smoke_test() {
        try (var context = prepareContext()) {
            context.eval("chi", "data Option[T] = Just(value: T) | Nothing");

            Assert.assertTrue(context.eval("chi", "Just(5) is Just").asBoolean());
            Assert.assertFalse(context.eval("chi", "Just(5) is Nothing").asBoolean());
            Assert.assertFalse(context.eval("chi", "Nothing is Just").asBoolean());
            Assert.assertTrue(context.eval("chi", "Nothing is Nothing").asBoolean());
        }
    }

    @Test
    public void is_should_also_check_type_name() {
        try (var context = prepareContext()) {
            context.eval("chi", "data Option[T] = Just(value: T) | Nothing");
            context.eval("chi", "data Other = Other");
            Assert.assertTrue(context.eval("chi", "Just(5) is Option").asBoolean());
            Assert.assertTrue(context.eval("chi", "Nothing is Option").asBoolean());
            Assert.assertFalse(context.eval("chi", "Other is Option").asBoolean());
        }
    }

    @Test
    public void funny_stuff() {
        try (var context = prepareContext()) {
            var result = context.eval("chi", """
                    fn inc(i:int):int { i + 1 }
                    fn double(f: (int) -> int): (int) -> int {
                        { i: int -> f(f(i)) }
                    }
                                        
                    double(inc)(5)
                    """);

            Assert.assertEquals(7, result.asInt());
        }
    }
}
