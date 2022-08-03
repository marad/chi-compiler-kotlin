import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static util.Utils.prepareContext;

public class ArraysTest {
    @Test
    public void can_create_an_array_with_default_elements() {
        try (var context = prepareContext()) {
            // given
            var result = context.eval("chi", """
                    import std/collections { array }
                    array[int](10, 5)
                    """);

            // result should be an array
            assertTrue(result.hasArrayElements());

            // of size 10
            assertEquals(10, result.getArraySize());

            // each element should be set to 5
            for (int i = 0; i < result.getArraySize(); i++) {
                assertEquals(5, result.getArrayElement(i).asInt());
            }
        }
    }

    @Test
    public void can_access_array_elements() {
        try (var context = prepareContext()) {
            // given
            var result = context.eval("chi", """
                    import std/collections { array }
                    val a = array[int](10, 5)
                    a[2]
                    """);

            // result should be third element of an array
            assertEquals(5, result.asInt());
        }
    }

    @Test
    public void can_assign_array_elements() {
        try (var context = prepareContext()) {
            // given
            var result = context.eval("chi", """
                    import std/collections { array }
                    val a = array[int](10, 5)
                    a[3] = 42
                    a
                    """);

            // element at index 3 should be 42
            assertEquals(42, result.getArrayElement(3).asInt());
        }
    }
}
