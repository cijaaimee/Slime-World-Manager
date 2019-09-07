package com.grinderwolf.swm.plugin.upgrade.v1_11;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.upgrade.Upgrade;

import java.util.HashMap;
import java.util.Map;

public class v1_11WorldUpgrade implements Upgrade {

    private static Map<String, String> oldToNewMap = new HashMap<>();
    private static Map<String, String> newToOldMap = new HashMap<>();

    static {
        rename("Furnace", "minecraft:furnace");
        rename("Chest", "minecraft:chest");
        rename("EnderChest", "minecraft:ender_chest");
        rename("RecordPlayer", "minecraft:jukebox");
        rename("Trap", "minecraft:dispenser");
        rename("Dropper", "minecraft:dropper");
        rename("Sign", "minecraft:sign");
        rename("MobSpawner", "minecraft:mob_spawner");
        rename("Music", "minecraft:noteblock");
        rename("Piston", "minecraft:piston");
        rename("Cauldron", "minecraft:brewing_stand");
        rename("EnchantTable", "minecraft:enchanting_table");
        rename("Airportal", "minecraft:end_portal");
        rename("Beacon", "minecraft:beacon");
        rename("Skull", "minecraft:skull");
        rename("DLDetector", "minecraft:daylight_detector");
        rename("Hopper", "minecraft:hopper");
        rename("Comparator", "minecraft:comparator");
        rename("FlowerPot", "minecraft:flower_pot");
        rename("Banner", "minecraft:banner");
        rename("Structure", "minecraft:structure_block");
        rename("EndGateway", "minecraft:end_gateway");
        rename("Control", "minecraft:command_block");
        rename(null, "minecraft:bed"); // Patch for issue s#62
    }

    private static void rename(String oldName, String newName) {
        if (oldName != null) {
            oldToNewMap.put(oldName, newName);
        }

        newToOldMap.put(newName, oldName);
    }

    @Override
    public void upgrade(CraftSlimeWorld world) {
        // 1.11 changed the way Tile Entities are named
        for (SlimeChunk chunk : world.getChunks().values()) {
            for (CompoundTag entityTag : chunk.getTileEntities()) {
                String oldType = entityTag.getAsStringTag("id").get().getValue();
                String newType = oldToNewMap.get(oldType);

                if (newType == null) {
                    if (newToOldMap.containsKey(oldType)) { // Maybe it's in the new format for some reason?
                        continue;
                    }

                    throw new IllegalStateException("Failed to find 1.11 upgrade for tile entity " + oldType);
                }

                entityTag.getValue().put("id", new StringTag("id", newType));
            }
        }
    }

    @Override
    public void downgrade(CraftSlimeWorld world) {
        for (SlimeChunk chunk : world.getChunks().values()) {
            for (CompoundTag entityTag : chunk.getTileEntities()) {
                String oldType = entityTag.getAsStringTag("id").get().getValue();
                String newType = newToOldMap.get(oldType);

                if (newType != null) {
                    entityTag.getValue().put("id", new StringTag("id", newType));
                }
            }
        }
    }
}
