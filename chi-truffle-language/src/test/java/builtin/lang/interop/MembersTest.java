package builtin.lang.interop;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Consumer;

import static util.Utils.prepareContext;


public class MembersTest {
    record TestContext(Context context) implements AutoCloseable {
        public Value eval(String code) {
            return context.eval("chi", """
                    import std/lang.interop as iop
                    data Foo = Foo(i: int, s: string)
                    val foo = Foo(10, "hello")
                    %s
                    """.formatted(code));
        }

        @Override
        public void close() {
            context.close();
        }
    }

    public void withTestContext(Consumer<TestContext> consumer) {
        try (var context = new TestContext(prepareContext())) {
            consumer.accept(context);
        }
    }

    @Test
    public void test_hasMembers() {
        withTestContext(context -> Assert.assertTrue(context.eval("iop.hasMembers(foo)").asBoolean()));
    }

    @Test
    public void test_getMembers() {
        withTestContext(context -> {
            var members = context.eval("iop.getMembers(foo, true)");
            Assert.assertTrue(members.hasArrayElements());
            Assert.assertEquals(2, members.getArraySize());
            Assert.assertEquals("i", members.getArrayElement(0).asString());
            Assert.assertEquals("s", members.getArrayElement(1).asString());
        });
    }

    @Test
    public void test_isMemberReadable() {
        withTestContext(context -> {
            Assert.assertTrue(context.eval("iop.isMemberReadable(foo, \"i\")").asBoolean());
            Assert.assertFalse(context.eval("iop.isMemberReadable(foo, \"other\")").asBoolean());
        });
    }

    @Test
    public void test_readMember() {
        withTestContext(context -> Assert.assertEquals(10, context.eval("iop.readMember(foo, \"i\")").asInt()));
    }

    @Test
    public void test_isMemberModifiable() {
        withTestContext(context -> Assert.assertTrue(context.eval("iop.isMemberModifiable(foo, \"i\")").asBoolean()));
    }

    @Test
    public void test_isMemberInsertable() {
        withTestContext(context -> Assert.assertFalse(context.eval("iop.isMemberInsertable(foo, \"i\")").asBoolean()));
    }

    @Test
    public void test_writeMember() {
        withTestContext(context -> {
            var result = context.eval("""
                    iop.writeMember(foo, "i", 5)
                    foo.i
                    """);
            Assert.assertEquals(5, result.asInt());
        });
    }

    @Test
    public void test_isMemberRemovable() {
        withTestContext(context -> Assert.assertFalse(context.eval("iop.isMemberRemovable(foo, \"i\")").asBoolean()));
    }

    @Test
    public void test_isMemberWritable() {
        withTestContext(context -> Assert.assertTrue(context.eval("iop.isMemberWritable(foo, \"i\")").asBoolean()));
    }

    @Test
    public void test_isMemberExisting() {
        withTestContext(context -> {
            Assert.assertTrue(context.eval("iop.isMemberExisting(foo, \"i\")").asBoolean());
            Assert.assertFalse(context.eval("iop.isMemberExisting(foo, \"other\")").asBoolean());
        });
    }

    @Test
    public void test_isMemberInvocable() {
        withTestContext(context -> {
            var result = context.eval("""
                    val system = iop.lookupHostSymbol("java.lang.System")
                    iop.isMemberInvocable(system, "currentTimeMillis")
                    """);
            Assert.assertTrue(result.asBoolean());
        });
    }

    @Test
    public void test_invokeMember() {
        withTestContext(context -> {
            var result = context.eval("""
                    fn func(i: int): int { i }
                    data Bar = Bar(f: (int) -> int)
                    val bar = Bar(func)
                    iop.invokeMember(bar, "f", array[any](1, 42))
                    """);

            Assert.assertEquals(42, result.asInt());

        });
    }
}
