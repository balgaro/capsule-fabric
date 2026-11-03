package capsule.compat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Thin read-only view over vanilla registries with the handful of Forge accessors this mod used. */
public final class ForgeRegistries {
    public static final Wrapper<Block> BLOCKS = new Wrapper<>(BuiltInRegistries.BLOCK);
    public static final Wrapper<Item> ITEMS = new Wrapper<>(BuiltInRegistries.ITEM);
    public static final Wrapper<Enchantment> ENCHANTMENTS = new Wrapper<>(BuiltInRegistries.ENCHANTMENT);
    public static final Wrapper<RecipeSerializer<?>> RECIPE_SERIALIZERS = new Wrapper<>(BuiltInRegistries.RECIPE_SERIALIZER);
    public static final Wrapper<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new Wrapper<>(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private ForgeRegistries() {
    }

    public static final class Wrapper<T> {
        private final Registry<T> registry;

        Wrapper(Registry<T> registry) {
            this.registry = registry;
        }

        public Registry<T> registry() {
            return registry;
        }

        public ResourceLocation getKey(T value) {
            return registry.getKey(value);
        }

        public T getValue(ResourceLocation key) {
            return registry.get(key);
        }

        public boolean containsKey(ResourceLocation key) {
            return registry.containsKey(key);
        }

        public Set<Map.Entry<ResourceKey<T>, T>> getEntries() {
            return registry.entrySet();
        }

        public Tags<T> tags() {
            return new Tags<>(registry);
        }
    }

    public static final class Tags<T> {
        private final Registry<T> registry;

        Tags(Registry<T> registry) {
            this.registry = registry;
        }

        public Iterable<T> getTag(TagKey<T> tag) {
            List<T> out = new ArrayList<>();
            registry.getTagOrEmpty(tag).forEach(holder -> out.add(holder.value()));
            return out;
        }
    }
}
