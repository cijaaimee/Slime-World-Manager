/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.plugin.upgrade;

import com.grinderwolf.swm.nms.CraftSlimeWorld;

public interface Upgrade {

    void upgrade(CraftSlimeWorld world);

    void downgrade(CraftSlimeWorld world);
}
