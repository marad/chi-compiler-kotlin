package gh.marad.chi.truffle.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import gh.marad.chi.truffle.nodes.ChiNode;
import gh.marad.chi.truffle.runtime.ChiFunction;

import java.util.function.Supplier;

public class FindFunction extends ChiNode {
    private final String name;
    @Child private ChiNode readFromLexicalScope;
    @Child private GetDefinedFunction readFromModule;

    public FindFunction(String name, ChiNode readFromLexicalScope, GetDefinedFunction readFromModule) {
        this.name = name;
        this.readFromLexicalScope = readFromLexicalScope;
        this.readFromModule = readFromModule;
    }

    @Override
    public ChiFunction executeFunction(VirtualFrame frame) {
        return tryAndReplace(readFromLexicalScope, frame,
                () -> tryAndReplace(readFromModule, frame, () -> {
                    CompilerDirectives.transferToInterpreter();
                    throw new RuntimeException("Function '%s' is not defined".formatted(name));
                }));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeFunction(frame);
    }

    private ChiFunction tryAndReplace(ChiNode getter, VirtualFrame frame, Supplier<ChiFunction> orElse) {
        try {
            var function = getter.executeFunction(frame);
            replace(getter);
            return function;
        } catch (Exception ex) {
            return orElse.get();
        }
    }
}
