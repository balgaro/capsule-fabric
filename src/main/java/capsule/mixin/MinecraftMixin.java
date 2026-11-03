package capsule.mixin;

import capsule.compat.CompatEvents;
import capsule.items.CapsuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Forge fired PlayerInteractEvent.LeftClickEmpty when the player left clicks in the air; Fabric has no such event.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Nullable
    public HitResult hitResult;
    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    private int missTime;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void capsule$leftClickInAir(CallbackInfoReturnable<Boolean> cir) {
        if (missTime <= 0 && player != null && hitResult != null && hitResult.getType() == HitResult.Type.MISS) {
            CapsuleItem.onLeftClick(new CompatEvents.LeftClick(player));
        }
    }
}
