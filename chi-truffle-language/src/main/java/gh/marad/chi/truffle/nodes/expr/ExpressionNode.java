package gh.marad.chi.truffle.nodes.expr;

import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import gh.marad.chi.truffle.ChiLanguage;
import gh.marad.chi.truffle.nodes.ChiNode;

@GenerateWrapper
public class ExpressionNode extends ChiNode implements InstrumentableNode {


    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new ExpressionNodeWrapper(this, probe);
    }

    private boolean hasRootTag = false;
    public void addRootTag() {
        hasRootTag = true;
    }
    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.RootTag.class || tag == StandardTags.RootBodyTag.class) {
            return hasRootTag;
        }
        return tag == StandardTags.ExpressionTag.class;
    }

//    @Override
//    public SourceSection getSourceSection() {
//        var source = Source.newBuilder(ChiLanguage.name, "dupa jasia", null).build();
//        return source.createSection(1);
//    }
}
