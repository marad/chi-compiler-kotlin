package util;

import gh.marad.chi.truffle.runtime.Unit;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class Utils {
    public static Value eval(String code) {
        var builder = Context.newBuilder("chi")
                              .allowExperimentalOptions(true)
                              .option("cpusampler", "true")
//                              .option("engine.BackgroundCompilation","false")
//                              .option("engine.CompilationFailureAction", "Throw")
//                              .option("engine.CompilationStatisticDetails", "true")
//                              .option("engine.CompileImmediately", "true")
//                              .option("engine.ShowInternalStackFrames", "true")
                              .option("engine.InstrumentBranches", "true")
//                              .option("engine.TraceCompilation", "true")
                ;
        try(var context = builder.build()) {
            return context.eval("chi", code);
        }
    }

    public static Unit evalUnit(String code) {
        try(var context = Context.create("chi")) {
            return context.eval("chi", code).as(Unit.class);
        }
    }

    public static boolean evalBoolean(String code) {
        return eval(code).asBoolean();
    }
}
