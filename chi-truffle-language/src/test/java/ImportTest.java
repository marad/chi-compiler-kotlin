import org.junit.Assert;
import org.junit.Ignore;
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

    @Test
    @Ignore("I'm not sure how to handle this when function is overloaded. Should I create some dispatch lambda function?")
    public void accessing_functions_should_work_through_aliased_package_name() {
        try (var context = prepareContext()) {
            // given
            context.eval("chi", """
                    package test/foo
                    fn testFn() {}
                    """);

            // when
            var result = context.eval("chi", """
                    import test/foo as foo
                    foo.testFn
                    """);

            // then
            Assert.assertTrue(result.canExecute());
        }
    }

    @Test
    public void last_import_wins() {
        try (var context = prepareContext()) {
            // given
            context.eval("chi", """
                    package test/a
                    val x = 10
                    """);

            context.eval("chi", """
                    package test/b
                    val x = 20
                    """);

            // when
            var result = context.eval("chi", """
                    import test/a { x }
                    import test/b { x }
                    x
                    """);

            // then
            Assert.assertEquals(20, result.asInt());
        }
    }
}
