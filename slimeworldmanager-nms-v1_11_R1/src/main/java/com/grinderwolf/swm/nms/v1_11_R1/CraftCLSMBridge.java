package com.grinderwolf.swm.nms.v1_11_R1;

import com.grinderwolf.swm.clsm.CLSMBridge;
import com.grinderwolf.swm.clsm.ClassModifier;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_11_R1.WorldServer;

@RequiredArgsConstructor
public class CraftCLSMBridge implements CLSMBridge {

    private final v1_11_R1SlimeNMS nmsInstance;

    @Override
    public Object[] getDefaultWorlds() {
        WorldServer defaultWorld = nmsInstance.getDefaultWorld();
        WorldServer netherWorld = nmsInstance.getDefaultNetherWorld();
        WorldServer endWorld = nmsInstance.getDefaultEndWorld();

        if (defaultWorld != null || netherWorld != null || endWorld != null) {
            return new WorldServer[] { defaultWorld, netherWorld, endWorld };
        }

        // Returning null will just run the original load world method
        return null;
    }

    public static void initialize(v1_11_R1SlimeNMS instance) {
        ClassModifier.setLoader(new CraftCLSMBridge(instance));
    }
}
