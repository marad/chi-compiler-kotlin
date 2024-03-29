package gh.marad.chi.truffle.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
public class ChiArray implements ChiValue {
    private final Object[] array;

    public ChiArray(int capacity, Object defaultValue) {
        array = new Object[capacity];
        Arrays.fill(array, defaultValue);
    }

    public ChiArray(int capacity) {
        array = new Object[capacity];
    }

    public ChiArray(Object[] array) {
        this.array = array;
    }

    public Object[] unsafeGetUnderlayingArray() {
        return array;
    }

    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public boolean isArrayElementReadable(long index) {
        return withinBounds(index);
    }

    @ExportMessage
    public Object readArrayElement(long index) throws InvalidArrayIndexException {
        assertIndexValid(index);
        return array[(int) index];
    }

    @ExportMessage
    public long getArraySize() {
        return array.length;
    }

    @ExportMessage
    public boolean isArrayElementModifiable(long index) {
        return withinBounds(index);
    }

    @ExportMessage
    public boolean isArrayElementInsertable(long index) {
        return false;
    }

    @ExportMessage
    public void writeArrayElement(long index, Object value) throws InvalidArrayIndexException {
        assertIndexValid(index);
        array[(int) index] = value;
    }

    @ExportMessage
    @Override
    @CompilerDirectives.TruffleBoundary
    public Object toDisplayString(boolean allowSideEffects) {
        var sb = new StringBuilder();
        sb.append("arrayOf(");
        var index = 0;
        for (Object element : array) {
            sb.append(element.toString());
            if (index < array.length - 1) {
                sb.append(", ");
            }
            index += 1;
        }
        sb.append(")");
        return sb.toString();
    }

    private boolean withinBounds(long index) {
        return 0 <= index && index < array.length;
    }

    private void assertIndexValid(long index) throws InvalidArrayIndexException {
        if (index < 0 || index >= array.length) {
            CompilerDirectives.transferToInterpreter();
            throw InvalidArrayIndexException.create(index);
        }
    }

    @ExportMessage
    @Override
    public boolean hasLanguage() {
        return ChiValue.super.hasLanguage();
    }

    @ExportMessage
    @Override
    public Class<? extends TruffleLanguage<?>> getLanguage() {
        return ChiValue.super.getLanguage();
    }
}
