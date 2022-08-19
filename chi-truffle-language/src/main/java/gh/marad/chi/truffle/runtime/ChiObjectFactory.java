package gh.marad.chi.truffle.runtime;

import gh.marad.chi.truffle.runtime.objects.ChiObjectDescriptor;

public interface ChiObjectFactory {
    ChiStaticObject create(ChiObjectDescriptor descriptor);
}
