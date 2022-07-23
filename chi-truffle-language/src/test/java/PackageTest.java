import org.junit.Assert;
import org.junit.Test;

import static util.Utils.prepareContext;

public class PackageTest {
    @Test
    public void should() {
        try(var context = prepareContext()) {
            context.eval("chi", """
                    package test/core
                    val x = 5
                    """);

            var result = context.eval("chi", """
                    test/core.x
                    """);

            Assert.assertEquals(5, result.asInt());
        }
    }
}
