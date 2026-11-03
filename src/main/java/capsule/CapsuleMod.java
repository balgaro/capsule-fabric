package capsule;

import capsule.blocks.CapsuleBlocks;
import capsule.command.CapsuleCommand;
import capsule.enchantments.CapsuleEnchantments;
import capsule.enchantments.RecallEnchant;
import capsule.itemGroups.CapsuleCreativeTabs;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.loot.CapsuleLootEntry;
import capsule.loot.CapsuleLootTableHook;
import capsule.loot.StarterLoot;
import capsule.network.CapsuleNetwork;
import capsule.plugins.claims.Claims;
import capsule.recipes.CapsuleRecipes;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.structure.CapsuleTemplateManager;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;

/**
 * Capsule, ported to Fabric. Original mod by Lythom (MIT licence), https://github.com/Lythom/capsule
 */
public class CapsuleMod implements ModInitializer {
    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CapsuleMod.class);

    public static final String MODID = "capsule";

    /**
     * Opens the label screen: the client entrypoint replaces this with the real screen.
     */
    public static Consumer<Player> openGuiScreenCommon = CapsuleMod::openGuiScreenServer;
    public static MinecraftServer server = null;

    @Override
    public void onInitialize() {
        // listen first: a common config is loaded as soon as it is registered, and Forge Config API Port fires "loading" then
        ModConfigEvents.loading(MODID).register(CapsuleMod::onModConfig);
        ModConfigEvents.reloading(MODID).register(CapsuleMod::onModConfig);
        ModConfig commonConfig = ForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        // safety net: the data reload needs the baked paths, never let it run on an unbaked config
        if (!Config.isBaked()) onModConfig(commonConfig);

        CapsuleBlocks.registerBlocks(null);
        CapsuleItems.registerItems(null);
        CapsuleCreativeTabs.registerTabs(null);
        CapsuleRecipes.registerRecipeSerializers(null);
        CapsuleEnchantments.registerEnchantments(null);
        CapsuleLootEntry.registerEntryType(null);

        CapsuleNetwork.setup();
        CapsuleItem.registerEvents();
        RecallEnchant.registerEvents();
        StarterLoot.registerEvents();
        CapsuleLootTableHook.registerEvents();

        ServerLifecycleEvents.SERVER_STARTING.register(CapsuleMod::serverStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CapsuleCommand.register(dispatcher));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new StructureSaverReloadListener());
    }

    private static void onModConfig(ModConfig config) {
        if (config.getSpec() == Config.COMMON_CONFIG) {
            Config.bakeConfig(config);
            LOGGER.debug("Baked COMMON_CONFIG");
        }
    }

    public static void serverStarting(MinecraftServer started) {
        server = started;
        Config.populateConfigFolders(server);
        Claims.loadAdapters();
        if (PrefabsBlueprintAggregatorRecipe.instance != null)
            PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(CapsuleMod.server.getResourceManager());
    }

    public static void openGuiScreenServer(Player player) {
    }
}

class StructureSaverReloadListener extends SimplePreparableReloadListener<Void> implements IdentifiableResourceReloadListener {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("capsule");
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(CapsuleMod.MODID, "structure_saver");
    }

    protected Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        try {
            StructureSaver.getRewardManager(pResourceManager).onResourceManagerReload(pResourceManager);
            for (CapsuleTemplateManager ctm : StructureSaver.CapsulesManagers.values()) {
                ctm.onResourceManagerReload(pResourceManager);
            }
        } catch (Throwable t) {
            LOGGER.error("Capsule could not prepare its templates on data reload (a failure here stalls the whole reload)", t);
            throw t;
        }
        return (Void) null;
    }

    protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {

    }
}
