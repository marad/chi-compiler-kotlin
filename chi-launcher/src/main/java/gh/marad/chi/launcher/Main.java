package gh.marad.chi.launcher;

import org.graalvm.polyglot.Context;

public class Main {
    public static void main(String[] args) {
        try(var context = Context.create("chi")) {
            context.eval("chi", "println(\"Hello World!\")");
        }
    }
}
