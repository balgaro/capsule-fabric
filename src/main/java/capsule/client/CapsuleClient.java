package capsule.client;

import capsule.CapsuleMod;
import capsule.blocks.CapsuleBlocks;
import capsule.compat.CompatEvents;
import capsule.compat.network.ClientSide;
import capsule.gui.LabelGui;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.network.CapsuleNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Client side setup of Capsule (what Forge did in FMLClientSetupEvent and the client event subscribers).
 */
public class CapsuleClient implements ClientModInitializer {
    private static ItemStack lastMainHand = ItemStack.EMPTY;

    @Override
    public void onInitializeClient() {
        CapsuleMod.openGuiScreenCommon = CapsuleClient::openGuiScreenClient;
        ClientSide.registerReceiver(CapsuleNetwork.wrapper);

        // color variants
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof CapsuleItem) {
                return CapsuleItem.getColorFromItemstack(stack, tintIndex);
            }
            return 0xFFFFFF;
        }, CapsuleItems.CAPSULE.get());
        ItemProperties.register(
                CapsuleItems.CAPSULE.get(),
                new ResourceLocation(CapsuleMod.MODID, "state"),
                (stack, world, entity, seed) -> CapsuleItem.getState(stack).getValue()
        );

        CapsuleBlocks.registerBlockEntitiesRenderer();
        CapsulePreviewHandler.registerClientEvents();

        // preview of the held capsule: ask the server for it when the main hand item changes
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Player player = mc.player;
            if (player == null) {
                lastMainHand = ItemStack.EMPTY;
                return;
            }
            ItemStack now = player.getMainHandItem();
            if (now != lastMainHand && !ItemStack.matches(now, lastMainHand)) {
                lastMainHand = now.copy();
                CapsuleItem.heldItemChange(new CompatEvents.EquipmentChange(player, now));
            }
        });
    }

    public static void openGuiScreenClient(Player player) {
        if (player.level().isClientSide) {
            Minecraft.getInstance().setScreen(new LabelGui(player));
        }
    }
}
