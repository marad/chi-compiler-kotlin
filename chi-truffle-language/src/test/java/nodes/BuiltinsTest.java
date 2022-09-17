package nodes;

import org.junit.Test;
import util.Utils;

public class BuiltinsTest {
    @Test
    public void should_invoke_builtins() {
        Utils.eval("""
                    import std/io { println }
                    println("Hello World!")
                """.stripIndent());
    }
}
