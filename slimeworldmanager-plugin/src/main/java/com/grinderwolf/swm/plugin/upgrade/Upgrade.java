package com.grinderwolf.swm.plugin.upgrade;

import com.grinderwolf.swm.nms.CraftSlimeWorld;

public interface Upgrade {

    void upgrade(CraftSlimeWorld world);
    void downgrade(CraftSlimeWorld world);
}
