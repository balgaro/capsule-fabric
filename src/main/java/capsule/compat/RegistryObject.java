package capsule.compat;

import java.util.function.Supplier;

/** Minimal stand-in for Forge's RegistryObject: holds an already registered value. */
public final class RegistryObject<T> implements Supplier<T> {
    private final T value;

    RegistryObject(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }
}
