package capsule.mixin;

import capsule.items.CapsuleItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge called Item.onEntityItemUpdate for every dropped item each tick. A thrown capsule uses it to fly to its target and
 * to deploy when it lands. Fabric has no such hook.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void capsule$onEntityItemUpdate(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (stack.getItem() instanceof CapsuleItem capsule && capsule.onEntityItemUpdate(stack, self)) {
            ci.cancel();
        }
    }
}
