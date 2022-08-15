import org.junit.Test;
import util.Utils;

public class ComplexTypesTest {
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
    public void should_define_and_assign_to_variant_types_smoke_test() {
        Utils.eval("""
                data Test = A(i: int) | B(s: string) | C
                val x: A = A(5)
                val y: B = B("hello")
                val z: C = C
                """);
    }

    @Test
    public void should_define_variant_with_multiple_parameters_smoke_test() {
        Utils.eval("""
                data Test = Test(i: int, s: string)
                """);
    }
}
