import org.junit.Test;

import static util.Utils.prepareContext;

public class GenericCompositeTypesTest {
    @Test
    public void foo() {
        try (var context = prepareContext()) {
            context.eval("chi", """
                    data Maybe[T] = Just(t: T) | Nothing
                    val mi = Just(5)
                    """);
        }
    }
}
