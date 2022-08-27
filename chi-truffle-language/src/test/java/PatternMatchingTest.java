import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class PatternMatchingTest {
    @Test
    public void smoke_test() {
        try (var context = prepareContext()) {
            context.eval("chi", "data Option[T] = Just(value: T) | Nothing");

            var result = context.eval("chi", """
                    match(Just(5)) {
                        Just(value) -> value
                        Nothing -> 0
                    }
                    """);

            Assert.assertEquals(5, result.asInt());
        }
    }
}
