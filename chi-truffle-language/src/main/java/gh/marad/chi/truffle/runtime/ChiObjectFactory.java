package gh.marad.chi.truffle.runtime;

import gh.marad.chi.truffle.runtime.objects.ChiObjectDescriptor;

public interface ChiObjectFactory {
    ChiObject create(ChiObjectDescriptor descriptor);
}
