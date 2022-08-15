import org.junit.Test;
import util.Utils;

public class ComplexTypesTest {
    @Test
    public void smoke_test() {
        Utils.eval("""
                data Test = A(i: int) | B(s: string) | C
                val x: Test = A(5)
                val y: Test = B("hello")
                val z: Test = C()
                """);
    }
}
