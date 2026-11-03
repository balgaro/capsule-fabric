package capsule.blocks;

import capsule.CapsuleMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import capsule.compat.IEventBus;
import capsule.compat.DeferredRegister;
import capsule.compat.ForgeRegistries;
import capsule.compat.RegistryObject;

public class CapsuleBlocks {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CapsuleMod.MODID);
    private static final DeferredRegister<Item> BLOCKITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CapsuleMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CapsuleMod.MODID);

    public static final RegistryObject<Block> CAPSULE_MARKER = BLOCKS.register("capsulemarker", BlockCapsuleMarker::new);
    public static final RegistryObject<BlockItem> CAPSULE_MARKER_ITEM = BLOCKITEMS.register("capsulemarker", () -> new BlockItem(CAPSULE_MARKER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BlockEntityCapture>> MARKER_TE = BLOCK_ENTITIES.register("capsulemarker_te", () -> BlockEntityType.Builder.of(BlockEntityCapture::new, CAPSULE_MARKER.get()).build(null));

    public static void registerBlocks(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        BLOCKITEMS.register(modEventBus);
    }

    @Environment(EnvType.CLIENT)
    public static void registerBlockEntitiesRenderer() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(MARKER_TE.get(), CaptureBER::new);
    }
}
