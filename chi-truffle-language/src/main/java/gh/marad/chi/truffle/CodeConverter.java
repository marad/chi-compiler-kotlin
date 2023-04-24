package gh.marad.chi.truffle;

import com.oracle.truffle.api.frame.FrameDescriptor;
import gh.marad.chi.core.compiled.Compiled;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.TODO;

public class CodeConverter {
    private final ChiLanguage language;
    private FrameDescriptor.Builder currentFdBuilder;

    public CodeConverter(ChiLanguage language) {
        this.language = language;
        this.currentFdBuilder = FrameDescriptor.newBuilder();
    }

    public ChiNode convert(Compiled code) {
        throw new TODO("Unhandled code conversion: %s".formatted(code));
    }
}
