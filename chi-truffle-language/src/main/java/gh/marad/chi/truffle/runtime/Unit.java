package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class Unit implements ChiValue {
    private Unit() {
    }

    public static final Unit instance = new Unit();

    @Override
    @ExportMessage
    public boolean hasLanguage() {
        return ChiValue.super.hasLanguage();
    }

    @Override
    @ExportMessage
    public Class<? extends TruffleLanguage<?>> getLanguage() {
        return ChiValue.super.getLanguage();
    }

    @Override
    @ExportMessage
    public Object toDisplayString(boolean allowSideEffects) {
        return "()";
    }
}
