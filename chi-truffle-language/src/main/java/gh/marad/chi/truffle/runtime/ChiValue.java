package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import gh.marad.chi.truffle.ChiLanguage;

@ExportLibrary(InteropLibrary.class)
public abstract class ChiValue implements TruffleObject {
    @ExportMessage
    public boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    public Class<? extends TruffleLanguage<?>> getLanguage() {
        return ChiLanguage.class;
    }

    @ExportMessage
    public abstract Object toDisplayString(boolean allowSideEffects);
}
