package com.grinderwolf.swm.plugin.upgrade;

import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import com.grinderwolf.swm.plugin.upgrade.v1_14.v1_14WorldUpgrade;

import java.util.HashMap;
import java.util.Map;

public class WorldUpgrader {

    private static final Map<Byte, Upgrade> upgrades = new HashMap<>();

    static {
        upgrades.put((byte) 0x05, new v1_14WorldUpgrade());
        // Todo we need a 1_14_WorldUpgrade class as well for 0x06
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
