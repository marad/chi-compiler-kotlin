import org.junit.Assert;
import org.junit.Test;
import util.Utils;

import java.util.List;

import static util.Utils.prepareContext;

public class CompositeTypesTest {
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

    @Test
    public void test_field_assignment() {
        try (var context = prepareContext()) {
            // when
            var result = context.eval("chi", """
                    data Test = Test(i: int)
                    val x = Test(10)
                    x.i = 42
                    x.i
                    """);

            // then
            Assert.assertEquals(42, result.asInt());
        }
    }

    @Test
    public void test_nested_field_assignment() {
        try (var context = prepareContext()) {
            // when
            var result = context.eval("chi", """
                    data Foo = Foo(i: int)
                    data Bar = Bar(foo: Foo)
                    data Baz = Baz(bar: Bar)
                    val x = Baz(Bar(Foo(10)))
                    x.bar.foo.i = 42
                    x.bar.foo.i
                    """);

            // then
            Assert.assertEquals(42, result.asInt());
        }
    }

    @Test
    public void should_allow_defining_recurring_types() {
        try (var context = prepareContext()) {
            // when
            var result = context.eval("chi", """
                    data Foo = Foo(foo: Foo) | Value(i: int)
                    Foo(Foo(Value(42)))
                    """);

            // then
            var value = result.getMember("foo")
                              .getMember("foo")
                              .getMember("i")
                              .asInt();

            Assert.assertEquals(42, value);
        }
    }

    @Test
    public void should_allow_fully_recursive_type_definition() {
        try (var context = prepareContext()) {
            context.eval("chi", """
                    data Option[T] = Just(value: T) | Nothing
                    data Pair[L,R] = Pair(left: L, right: R)
                    data Iterator[T] = Iterator(hasNext: () -> bool, next: () -> Option[Pair[T, Iterator[T]]])
                                        
                    fn iterator[T](): Iterator[T] {
                        val hasNext = fn(): bool { false }
                        val next = fn(): Option[Pair[T, Iterator[T]]] {
                            Nothing
                        }
                        Iterator(hasNext, next)
                    }
                    """);
        }
    }
}
