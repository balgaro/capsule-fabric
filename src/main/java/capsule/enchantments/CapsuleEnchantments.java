package capsule.enchantments;

import capsule.CapsuleMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import capsule.compat.IEventBus;
import capsule.compat.DeferredRegister;
import capsule.compat.ForgeRegistries;
import capsule.compat.RegistryObject;

import java.util.function.Predicate;

public class CapsuleEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, CapsuleMod.MODID);
    public static final RegistryObject<Enchantment> RECALL = ENCHANTMENTS.register("recall", RecallEnchant::new);

    public static final Predicate<Entity> hasRecallEnchant = (Entity entityIn) ->
            entityIn instanceof ItemEntity itemEntity && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(CapsuleEnchantments.RECALL.get(), itemEntity.getItem()) > 0;

    public static void registerEnchantments(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }

    public static RecallEnchant CreateRecall() {
        return new RecallEnchant();
    }
}
