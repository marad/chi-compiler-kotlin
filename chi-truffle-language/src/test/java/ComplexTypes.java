import org.junit.Test;
import util.Utils;

public class ComplexTypes {
    @Test
    public void smoke_test() {
        Utils.eval("""
                data Test = A(i: int) | B(s: string) | C
                val x: Test = A(5)
                val y: Test = B(10)
                val z: Test = C
                """);
    }
}
