package capsule.recipes;

import capsule.CapsuleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import capsule.compat.IEventBus;
import capsule.compat.DeferredRegister;
import capsule.compat.ForgeRegistries;
import capsule.compat.RegistryObject;

public class CapsuleRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CapsuleMod.MODID);

    public static final RegistryObject<BlueprintCapsuleRecipe.Serializer> BLUEPRINT_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("blueprint_capsule", BlueprintCapsuleRecipe.Serializer::new);
    public static final RegistryObject<SimpleCraftingRecipeSerializer<BlueprintChangeRecipe>> BLUEPRINT_CHANGE_SERIALIZER = RECIPE_SERIALIZERS.register("blueprint_change", () -> new SimpleCraftingRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final RegistryObject<SimpleCraftingRecipeSerializer<ClearCapsuleRecipe>> CLEAR_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("clear_capsule", () -> new SimpleCraftingRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final RegistryObject<SimpleCraftingRecipeSerializer<DyeCapsuleRecipe>> DYE_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("dye_capsule", () -> new SimpleCraftingRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final RegistryObject<RecoveryCapsuleRecipe.Serializer> RECOVERY_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("recovery_capsule", RecoveryCapsuleRecipe.Serializer::new);
    public static final RegistryObject<UpgradeCapsuleRecipe.Serializer> UPGRADE_CAPSULE_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade_capsule", UpgradeCapsuleRecipe.Serializer::new);
    public static final RegistryObject<PrefabsBlueprintAggregatorRecipe.Serializer> PREFABS_AGGREGATOR_SERIALIZER = RECIPE_SERIALIZERS.register("aggregate_all_prefabs", PrefabsBlueprintAggregatorRecipe.Serializer::new);

    public static void registerRecipeSerializers(IEventBus event) {
        RECIPE_SERIALIZERS.register(event);
        CustomIngredientSerializer.register(CapsuleIngredient.Serializer.INSTANCE);
    }
}
