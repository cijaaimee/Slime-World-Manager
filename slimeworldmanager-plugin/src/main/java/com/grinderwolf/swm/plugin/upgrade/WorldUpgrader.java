package com.grinderwolf.swm.plugin.upgrade;

import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import com.grinderwolf.swm.plugin.upgrade.v1_9.v1_11WorldUpgrade;
import com.grinderwolf.swm.plugin.upgrade.v1_9.v1_9WorldUpgrade;

import java.util.HashMap;
import java.util.Map;

public class WorldUpgrader {

    private static final Map<Byte, Upgrade> upgrades = new HashMap<>();

    static {
        upgrades.put((byte) 0x02, new v1_9WorldUpgrade());
        upgrades.put((byte) 0x03, new v1_11WorldUpgrade());
    }

    public static void upgradeWorld(CraftSlimeWorld world) {
        byte serverVersion = SWMPlugin.getInstance().getNms().getWorldVersion();

        for (byte ver = (byte) (world.getVersion() + 1); ver <= serverVersion; ver++) {
            Upgrade upgrade = upgrades.get(ver);

            if (upgrade == null) {
                Logging.warning("Missing world upgrader for version " + ver + ". World will not be upgraded.");
                continue;
            }

            upgrade.upgrade(world);
        }

        world.setVersion(serverVersion);
    }

    public static void downgradeWorld(CraftSlimeWorld world) {
        byte serverVersion = SWMPlugin.getInstance().getNms().getWorldVersion();

        for (byte ver = world.getVersion(); ver > serverVersion; ver--) {
            Upgrade upgrade = upgrades.get(ver);

            if (upgrade == null) {
                Logging.warning("Missing world upgrader for version " + ver + ". World will not be downgraded.");
                continue;
            }

            upgrade.downgrade(world);
        }

        world.setVersion(serverVersion);
    }
}
