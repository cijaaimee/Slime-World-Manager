/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.nms.v1_8_R3;

import com.grinderwolf.swm.clsm.CLSMBridge;
import com.grinderwolf.swm.clsm.ClassModifier;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.WorldServer;

@RequiredArgsConstructor
public class CraftCLSMBridge implements CLSMBridge {

    private final v1_8_R3SlimeNMS nmsInstance;

    static void initialize(v1_8_R3SlimeNMS instance) {
        ClassModifier.setLoader(new CraftCLSMBridge(instance));
    }

    @Override
    public Object[] getDefaultWorlds() {
        WorldServer defaultWorld = nmsInstance.getDefaultWorld();
        WorldServer netherWorld = nmsInstance.getDefaultNetherWorld();
        WorldServer endWorld = nmsInstance.getDefaultEndWorld();

        if (defaultWorld != null || netherWorld != null || endWorld != null) {
            return new WorldServer[] {defaultWorld, netherWorld, endWorld};
        }

        // Returning null will just run the original load world method
        return null;
    }

    @Override
    public boolean isCustomWorld(Object world) {
        return world instanceof CustomWorldServer;
    }

    @Override
    public boolean skipWorldAdd(Object world) {
        if (!isCustomWorld(world) || nmsInstance.isLoadingDefaultWorlds()) {
            return false;
        }

        CustomWorldServer worldServer = (CustomWorldServer) world;
        return !worldServer.isReady();
    }
}
