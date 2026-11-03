package capsule.mixin;

import capsule.StructureSaver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge let Capsule cancel EntityJoinLevelEvent while a capture is running, so that the items and experience popped
 * by the removed blocks (already stored in the capsule) never appear. This is the Fabric equivalent.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void capsule$preventCaptureDrops(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (StructureSaver.isPreventedDrop(entity)) {
            cir.setReturnValue(false);
        }
    }
}
