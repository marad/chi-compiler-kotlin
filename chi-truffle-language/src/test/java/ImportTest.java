import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class ImportTest {
    @Test
    public void type_can_be_imported_with_constructors() {
        try (var context = prepareContext()) {
            // given
            context.eval("chi", """
                    package test/types
                    data Foo = Bar(i: int)
                    """);

            // when
            var result = context.eval("chi", """
                    import test/types { Foo }
                    val test: Foo = Bar(42)
                    test.i
                    """);

            // then
            Assert.assertEquals(42, result.asInt());
        }
    }

    @Test
    public void constructor_can_be_imported_separately() {
        try (var context = prepareContext()) {
            // given
            context.eval("chi", """
                    package test/types
                    data Foo = Bar(i: int)
                    """);

            // when
            var result = context.eval("chi", """
                    import test/types { Foo, Bar }
                    val test: Foo = Bar(42)
                    test.i
                    """);

            // then
            Assert.assertEquals(42, result.asInt());
        }
    }

    @Test
    public void type_and_constructor_can_be_aliased() {
        try (var context = prepareContext()) {
            // given
            context.eval("chi", """
                    package test/types
                    data Foo = Bar(i: int)
                    """);

            // when
            var result = context.eval("chi", """
                    import test/types { Foo as AliasedFoo , Bar as AliasedBar }
                    val test: AliasedFoo = AliasedBar(42)
                    test.i
                    """);

            // then
            Assert.assertEquals(42, result.asInt());
        }
    }
}