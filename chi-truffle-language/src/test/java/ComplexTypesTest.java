import org.junit.Assert;
import org.junit.Test;
import util.Utils;

import java.util.List;

import static util.Utils.prepareContext;

public class ComplexTypesTest {
    @Test
    public void should_define_and_assign_variants_to_type_smoke_test() {
        Utils.eval("""
                data Test = A(i: int) | B(s: string) | C
                val x: Test = A(5)
                val y: Test = B("hello")
                val z: Test = C
                """);
    }

    @Test
    public void should_define_and_assign_to_variant_types_smoke_test() {
        Utils.eval("""
                data Test = A(i: int) | B(s: string) | C
                val x: A = A(5)
                val y: B = B("hello")
                val z: C = C
                """);
    }

    @Test
    public void test_interop() {
        try (var context = prepareContext()) {
            // when
            var result = context.eval("chi", """
                    data Test = Test(i: int, s: string)
                    Test(42, "hello")
                    """);

            // then
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.getMemberKeys().containsAll(List.of("i", "s")));
            Assert.assertTrue(result.hasMember("i"));
            Assert.assertTrue(result.hasMember("s"));
            Assert.assertEquals(42, result.getMember("i").asInt());
            Assert.assertEquals("hello", result.getMember("s").asString());

            // and when
            result.putMember("i", 5L);

            // then
            Assert.assertEquals(5, result.getMember("i").asInt());
        }
    }

    @Test
    public void test_field_access() {
        try (var context = prepareContext()) {
            // when
            var result = context.eval("chi", """
                    data Test = Test(i: int)
                    val x = Test(10)
                    x.i
                    """);

            // then
            Assert.assertEquals(10, result.asInt());
        }
    }

    @Test
    public void test_invoking_properties_as_functions() {
        try (var context = prepareContext()) {
            // when
            var result = context.eval("chi", """
                    data Test = Test(f: () -> int)
                    val x = Test(fn(): int { 42 })
                    x.f()
                    """);

            // then
            Assert.assertEquals(42, result.asInt());
        }
    }

}
