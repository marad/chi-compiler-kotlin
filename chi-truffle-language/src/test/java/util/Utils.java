package util;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class Utils {
    public static Context prepareContext() {
        return Context.newBuilder("chi")
                      .allowExperimentalOptions(true)
                      .option("cpusampler", "true")
                      .option("cpusampler.ShowTiers", "true")
//                             .option("cpusampler.SampleContextInitialization", "true")
//                             .option("cpusampler.Period", "500")
//                              .option("cpusampler.SummariseThreads", "true")
//                             .option("engine.TraceCompilation", "true")
//                      .option("engine.TracePerformanceWarnings", "trivial,instanceof,frame_merge,store")
//                      .option("engine.TreatPerformanceWarningsAsErrors","trivial,instanceof,frame_merge,store")
                      .option("engine.TracePerformanceWarnings", "all")
                      .option("engine.TreatPerformanceWarningsAsErrors", "all")

//                             .option("engine.TraceTransferToInterpreter", "true")
//                             .option("engine.Profiling", "true")
//                              .option("engine.SpecializationStatistics", "true")
//                              .option("engine.TraceCompilationDetails", "true")
//                      .option("engine.CompilationFailureAction", "Diagnose")

//                              .option("engine.TraceDeoptimizeFrame", "true")
//                              .option("engine.TraceNodeExpansion","true")
//                              .option("engine.TraceSplitting","true")
//                              .option("engine.TraceSplittingSummary","true")

//                      .option("engine.TraceCompilation", "true")
//                      .option("engine.CompileImmediately", "true")
//                      .option("engine.BackgroundCompilation","false")
                      .option("engine.CompilationFailureAction", "Print")

//                              .option("engine.CompilationFailureAction", "Throw")
//                              .option("engine.CompilationStatisticDetails", "true")
//                              .option("engine.CompileImmediately", "true")
//                              .option("engine.ShowInternalStackFrames", "true")
//                              .option("engine.InstrumentBranches", "true")
//                              .option("engine.InstrumentFilter", "*.*.*")
//                       .option("log.file", "polyglot.txt")
//                       .option("engine.InliningRecursionDepth", "10")
//                       .option("engine.TraceInlining", "true")
//                       .option("engine.Inlining", "true")
                      .build();

    }

    public static Value eval(String code) {
        try (var context = prepareContext()) {
            return context.eval("chi", code);
        }
    }

    public static boolean evalBoolean(String code) {
        return eval(code).asBoolean();
    }

    public static int evalInt(String code) {
        return eval(code).asInt();
    }
}
