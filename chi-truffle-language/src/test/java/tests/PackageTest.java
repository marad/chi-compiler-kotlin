package tests;

import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class PackageTest {
    @Test
    public void should_get_variable_value_from_other_package() {
        try (var context = prepareContext()) {
            context.eval("chi", """
                    package test/core
                    pub val x = 5
                    """);

            var result = context.eval("chi", """
                    import test/core { x }
                    x
                    """);

            Assert.assertEquals(5, result.asInt());
        }
    }

    @Test
    public void should_get_function_from_other_package() {
        try (var context = prepareContext()) {
            context.eval("chi", """
                    package test/core
                    pub fn foo(): int { 5 }
                    """);

            var result = context.eval("chi", """
                    import test/core { foo }
                    foo()
                    """);

            Assert.assertEquals(5, result.asInt());
        }
    }
}
