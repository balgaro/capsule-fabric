package capsule.plugins.securitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * SecurityCraft blocks can only be taken by their owner on Forge. SecurityCraft does not exist for Fabric, so on
 * Fabric there is nothing to check.
 */
public class SecurityCraftOwnerCheck {

    public static boolean canTakeBlock(ServerLevel worldserver, BlockPos blockPos, Player player) {
        return true;
    }
}
