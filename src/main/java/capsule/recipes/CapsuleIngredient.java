package capsule.recipes;

import capsule.compat.ForgeRegistries;
import capsule.items.CapsuleItem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Matches a capsule by its state (empty, linked, blueprint...), not by its exact NBT.
 * Recipes use it with <code>"fabric:type": "capsule:ingredient"</code>.
 */
public class CapsuleIngredient implements CustomIngredient {
    private final ItemStack referenceStack;

    protected CapsuleIngredient(ItemStack stack) {
        this.referenceStack = stack;
    }

    @Override
    public boolean test(ItemStack input) {
        if (input == null || !(input.getItem() instanceof CapsuleItem))
            return false;
        return this.referenceStack.getItem() == input.getItem()
                && CapsuleItem.hasState(input, CapsuleItem.getState(referenceStack))
                && CapsuleItem.isBlueprint(input) == CapsuleItem.isBlueprint(referenceStack);
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        return List.of(referenceStack);
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements CustomIngredientSerializer<CapsuleIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation("capsule", "ingredient");

        @Override
        public ResourceLocation getIdentifier() {
            return ID;
        }

        @Override
        public CapsuleIngredient read(JsonObject json) {
            return new CapsuleIngredient(itemStackFromJson(json));
        }

        @Override
        public void write(JsonObject json, CapsuleIngredient ingredient) {
            json.addProperty("item", ForgeRegistries.ITEMS.getKey(ingredient.referenceStack.getItem()).toString());
            json.addProperty("count", ingredient.referenceStack.getCount());
            if (ingredient.referenceStack.hasTag()) json.addProperty("nbt", ingredient.referenceStack.getTag().toString());
        }

        @Override
        public CapsuleIngredient read(FriendlyByteBuf buffer) {
            return new CapsuleIngredient(buffer.readItem());
        }

        @Override
        public void write(FriendlyByteBuf buffer, CapsuleIngredient ingredient) {
            buffer.writeItem(ingredient.referenceStack);
        }

        /**
         * Reads {"item": id, "count": n, "nbt": {...} or "{snbt}"}, what Forge's CraftingHelper.getItemStack(json, true) did.
         */
        private static ItemStack itemStackFromJson(JsonObject json) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || !ForgeRegistries.ITEMS.containsKey(id)) {
                throw new JsonSyntaxException("Unknown item '" + id + "'");
            }
            ItemStack stack = new ItemStack(item, GsonHelper.getAsInt(json, "count", 1));
            if (json.has("nbt")) {
                JsonElement nbt = json.get("nbt");
                try {
                    String snbt = nbt.isJsonObject() ? new Gson().toJson(nbt) : GsonHelper.convertToString(nbt, "nbt");
                    CompoundTag tag = TagParser.parseTag(snbt);
                    stack.setTag(tag);
                } catch (CommandSyntaxException e) {
                    throw new JsonSyntaxException("Invalid nbt tag: " + e.getMessage());
                }
            }
            return stack;
        }
    }
}
