package gh.marad.chi.truffle.nodes.expr.flow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static util.Utils.prepareContext;

public class EffectsTest {
    @Test
    public void smoke_test() {
        try (var context = prepareContext()) {
            var code = """
                    effect readString(fileName: string): string
                                        
                    fn someFunc() {
                      println(readString("somefile.txt"))
                    }
                                        
                    handle {
                        someFunc()
                    } with {
                        readString(fileName) -> resume(fileName)
                    }
                    """;
            var result = context.eval("chi", code);

            assertEquals("somefile.txt", result.asString());
        }
    }
}