package util;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class Utils {
    public static Value eval(String code) {
        try(var context = Context.create("chi")) {
            return context.eval("chi", code);
        }
    }

    public static boolean evalBoolean(String code) {
        return eval(code).asBoolean();
    }
}
