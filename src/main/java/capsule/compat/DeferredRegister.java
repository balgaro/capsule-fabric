package capsule.compat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Stand-in for Forge's DeferredRegister. Fabric registries are open while mods initialise, so entries are
 * registered immediately instead of being queued for a registry event.
 */
@SuppressWarnings("unchecked")
public final class DeferredRegister<T> {
    private final Registry<T> registry;
    private final String modId;

    private DeferredRegister(Registry<T> registry, String modId) {
        this.registry = registry;
        this.modId = modId;
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String modId) {
        return new DeferredRegister<>(registry, modId);
    }

    public static <T> DeferredRegister<T> create(ForgeRegistries.Wrapper<T> wrapper, String modId) {
        return new DeferredRegister<>(wrapper.registry(), modId);
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String modId) {
        return new DeferredRegister<>((Registry<T>) BuiltInRegistries.REGISTRY.get(key.location()), modId);
    }

    public <I extends T> RegistryObject<I> register(String name, Supplier<I> factory) {
        I value = factory.get();
        Registry.register(registry, new ResourceLocation(modId, name), value);
        return new RegistryObject<>(value);
    }

    /** Forge needs the mod event bus here; Fabric has nothing to do. */
    public void register(IEventBus ignored) {
    }
}
