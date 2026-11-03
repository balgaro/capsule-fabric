package capsule.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Small event objects shaped like the Forge events this mod handled, so the handler bodies stay unchanged.
 * Fabric callbacks build them and then act on the result.
 */
public final class CompatEvents {
    private CompatEvents() {
    }

    /** Left click on a block or on thin air. */
    public static class LeftClick {
        private final Player player;
        private boolean canceled = false;

        public LeftClick(Player player) {
            this.player = player;
        }

        public Player getEntity() {
            return player;
        }

        public Level getLevel() {
            return player.level();
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    /** Server tick of one player. */
    public static class PlayerTick {
        public final Player player;

        public PlayerTick(Player player) {
            this.player = player;
        }
    }

    /** The main hand item of a player changed. */
    public static class EquipmentChange {
        private final Player player;
        private final ItemStack to;

        public EquipmentChange(Player player, ItemStack to) {
            this.player = player;
            this.to = to;
        }

        public Player getEntity() {
            return player;
        }

        public EquipmentSlot getSlot() {
            return EquipmentSlot.MAINHAND;
        }

        public ItemStack getTo() {
            return to;
        }
    }
}
