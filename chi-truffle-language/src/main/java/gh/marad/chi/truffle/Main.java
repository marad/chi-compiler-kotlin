package gh.marad.chi.truffle;

import org.graalvm.polyglot.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        var script = Files.readString(Path.of("examples", "fib.chi"));
        try(var context = Context.create("chi")) {
            var result = context.eval("chi", script);
            System.out.println("Result: " + result);
        }
    }
}
