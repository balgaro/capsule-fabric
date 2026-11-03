package capsule.loot;

import capsule.Config;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class CapsuleLootTableHook {

    /** A pool with one capsule loot entry per configured folder, as added to the loot tables. */
    public static LootPool capsulePool() {
        return capsulePoolBuilder().build();
    }

    private static LootPool.Builder capsulePoolBuilder() {
        LootPool.Builder builder = LootPool.lootPool()
                .setBonusRolls(ConstantValue.exactly(0))
                .setRolls(ConstantValue.exactly(1));
        for (Config.LootPathData data : Config.lootTemplatesData.values()) {
            builder.add(CapsuleLootEntry.builder(data.path));
        }
        return builder;
    }

    public static void registerEvents() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (!Config.lootTablesList.contains(id.toString()))
                return;

            // add a pool with one capsule loot entry per folder
            tableBuilder.pool(capsulePool());
        });
    }
}
