package gh.marad.chi.truffle;

import org.graalvm.polyglot.Context;

public class Main {
    public static void main(String[] args) {
        var result = Context.create("chi").eval("chi", "50");
        System.out.printf("Result: %s%n", result.asInt());
    }
}
