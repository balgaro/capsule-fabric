package capsule.compat;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The slot based view of an inventory this mod used from Forge (IItemHandler), backed by whatever the
 * platform offers: a Fabric Transfer API item storage (chests, barrels, modded machines) or a plain vanilla container.
 */
public interface IItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    /** Takes up to amount items out of the slot and returns what was taken. */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    /** Puts the stack in the inventory (Forge preferred the given slot first) and returns what did not fit. */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** The inventory at pos, or null when the block has none. */
    @Nullable
    static IItemHandler find(Level level, BlockPos pos) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, null);
        if (storage != null) return new StorageHandler(storage);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container) return new ContainerHandler(container, container.getContainerSize());
        return null;
    }

    static boolean hasInventory(Level level, BlockPos pos) {
        return find(level, pos) != null;
    }

    /** The main inventory of a player (hotbar and 27 slots), without armor and off hand. */
    static IItemHandler ofPlayer(net.minecraft.world.entity.player.Player player) {
        return new ContainerHandler(player.getInventory(), 36);
    }

    class ContainerHandler implements IItemHandler {
        private final Container container;
        private final int slots;

        public ContainerHandler(Container container, int slots) {
            this.container = container;
            this.slots = Math.min(slots, container.getContainerSize());
        }

        @Override
        public int getSlots() {
            return slots;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return container.getItem(slot);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack inSlot = container.getItem(slot);
            if (inSlot.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int taken = Math.min(amount, inSlot.getCount());
            ItemStack result = inSlot.copy();
            result.setCount(taken);
            if (!simulate) {
                inSlot.shrink(taken);
                if (inSlot.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
                container.setChanged();
            }
            return result;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack remaining = stack.copy();
            for (int i = 0; i < slots && !remaining.isEmpty(); i++) {
                int target = i == 0 ? slot : (i <= slot ? i - 1 : i);
                if (target < 0 || target >= slots) continue;
                ItemStack inSlot = container.getItem(target);
                if (inSlot.isEmpty()) {
                    if (!container.canPlaceItem(target, remaining)) continue;
                    int moved = Math.min(remaining.getCount(), Math.min(container.getMaxStackSize(), remaining.getMaxStackSize()));
                    if (!simulate) {
                        ItemStack placed = remaining.copy();
                        placed.setCount(moved);
                        container.setItem(target, placed);
                    }
                    remaining.shrink(moved);
                } else if (ItemStack.isSameItemSameTags(inSlot, remaining)) {
                    int room = Math.min(container.getMaxStackSize(), inSlot.getMaxStackSize()) - inSlot.getCount();
                    int moved = Math.min(room, remaining.getCount());
                    if (moved > 0) {
                        if (!simulate) inSlot.grow(moved);
                        remaining.shrink(moved);
                    }
                }
            }
            if (!simulate) container.setChanged();
            return remaining;
        }
    }

    class StorageHandler implements IItemHandler {
        private final Storage<ItemVariant> storage;

        public StorageHandler(Storage<ItemVariant> storage) {
            this.storage = storage;
        }

        private List<StorageView<ItemVariant>> views() {
            List<StorageView<ItemVariant>> views = new ArrayList<>();
            for (StorageView<ItemVariant> view : storage) views.add(view);
            return views;
        }

        @Override
        public int getSlots() {
            return views().size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            List<StorageView<ItemVariant>> views = views();
            if (slot < 0 || slot >= views.size()) return ItemStack.EMPTY;
            StorageView<ItemVariant> view = views.get(slot);
            if (view.isResourceBlank() || view.getAmount() <= 0) return ItemStack.EMPTY;
            return view.getResource().toStack((int) Math.min(Integer.MAX_VALUE, view.getAmount()));
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            List<StorageView<ItemVariant>> views = views();
            if (slot < 0 || slot >= views.size()) return ItemStack.EMPTY;
            StorageView<ItemVariant> view = views.get(slot);
            if (view.isResourceBlank()) return ItemStack.EMPTY;
            ItemVariant variant = view.getResource();
            try (Transaction tx = Transaction.openOuter()) {
                long extracted = view.extract(variant, amount, tx);
                if (!simulate) tx.commit();
                return extracted <= 0 ? ItemStack.EMPTY : variant.toStack((int) extracted);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
                ItemStack rest = stack.copy();
                rest.shrink((int) inserted);
                return rest;
            }
        }
    }
}
