package capsule.loot;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.helpers.Capsule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public class StarterLoot {

    protected static final Logger LOGGER = LogManager.getLogger(StarterLoot.class);

    public static StarterLoot instance = new StarterLoot();

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> playerLogin(handler.getPlayer()));
    }

    /** Fabric has no persistent player data like Forge: a scoreboard tag (saved with the player) remembers the gift. */
    private static final String RECEIVED_STARTER_TAG = "capsule:receivedStarter";

    public static void playerLogin(ServerPlayer player) {
        if (!player.level().isClientSide) {
            if (StringUtil.isNullOrEmpty(Config.starterMode) || Config.starterTemplatesList == null || Config.starterTemplatesList.isEmpty()) {
                LOGGER.info("Capsule starters are disabled in capsule.cfg. To enable, set starterMode to 'all' or 'random' and set a directory path with structures for starterTemplatesPath.");
                return;
            }
            boolean alreadyReceived = player.getTags().contains(RECEIVED_STARTER_TAG);
            LOGGER.info("playerLogin: " + (alreadyReceived ? "already received starters" : "giving starters now"));
            if (!alreadyReceived) {
                if ("all".equals(Config.starterMode.toLowerCase())) {
                    giveAllStarters(player, Config.starterTemplatesList);
                    player.addTag(RECEIVED_STARTER_TAG);

                } else if ("random".equals(Config.starterMode)) {
                    giveAllStarters(player, Collections.singletonList(
                            Config.starterTemplatesList.get(
                                    (int) (Math.random() * Config.starterTemplatesList.size())
                            )
                    ));
                    player.addTag(RECEIVED_STARTER_TAG);
                }
            }

        }
    }

    public static void giveAllStarters(ServerPlayer player, List<String> allStartersToGive) {
        for (String templatePath : allStartersToGive) {
            ItemStack starterCapsule = Capsule.createLinkedCapsuleFromReward(templatePath, player);
            int stackIdx = player.getInventory().getFreeSlot();
            if (stackIdx < 0 || stackIdx >= player.getInventory().getContainerSize()) {
                Capsule.throwCapsule(starterCapsule, player, player.blockPosition());
            } else {
                try {
                    player.getInventory().setItem(stackIdx, starterCapsule);
                } catch (Exception e) {
                    Capsule.throwCapsule(starterCapsule, player, player.blockPosition());
                }
            }
        }
    }
}
