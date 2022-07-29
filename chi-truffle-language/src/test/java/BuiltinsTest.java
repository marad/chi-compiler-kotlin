import org.junit.Test;
import util.Utils;

public class BuiltinsTest {
    @Test
    public void should_invoke_builtins() {
        Utils.eval("std/io.println(\"Hello World!\")");
    }
}
