package com.grinderwolf.swm.nms.v1_16_R2;

import com.grinderwolf.swm.clsm.CLSMBridge;
import com.grinderwolf.swm.clsm.ClassModifier;
import com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer;
import com.grinderwolf.swm.nms.v1_16_R2.v1_16_R2SlimeNMS;
import net.minecraft.server.v1_16_R2.Chunk;
import net.minecraft.server.v1_16_R2.IChunkAccess;
import net.minecraft.server.v1_16_R2.ProtoChunkExtension;
import net.minecraft.server.v1_16_R2.WorldServer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CraftCLSMBridge implements CLSMBridge {

    private static final Logger LOGGER = LogManager.getLogger("SWM Chunk Loader");

    private final com.grinderwolf.swm.nms.v1_16_R2.v1_16_R2SlimeNMS nmsInstance;

    @Override
    public Object getChunk(Object worldObject, int x, int z) {
        if (!(worldObject instanceof com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer)) {
//            System.out.println("world is of type " + worldObject.getClass().getName());

            return null; // Returning null will just run the original getChunk method
        }

        com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer world = (com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer) worldObject;

        return world.getChunk(x, z);
    }

    @Override
    public boolean saveChunk(Object world, Object chunkAccess) {
        if (!(world instanceof com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer)) {
            return false; // Returning false will just run the original saveChunk method
        }

        if (!(chunkAccess instanceof ProtoChunkExtension || chunkAccess instanceof Chunk) || !((IChunkAccess) chunkAccess).isNeedsSaving()) {
            // We're only storing fully-loaded chunks that need to be saved
            return true;
        }

        Chunk chunk;

        if (chunkAccess instanceof ProtoChunkExtension) {
            chunk = ((ProtoChunkExtension) chunkAccess).u();
        } else {
            chunk = (Chunk) chunkAccess;
        }


        ((com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer) world).saveChunk(chunk);
        chunk.setNeedsSaving(false);

        return true;
    }

    @Override
    public Object[] getDefaultWorlds() {
        WorldServer defaultWorld = nmsInstance.getDefaultWorld();
        WorldServer netherWorld = nmsInstance.getDefaultNetherWorld();
        WorldServer endWorld = nmsInstance.getDefaultEndWorld();
        LOGGER.info("getDefaultWorlds");
        if (defaultWorld != null || netherWorld != null || endWorld != null) {
            LOGGER.info("not null, returning: " + defaultWorld + ", " + netherWorld + ", " + endWorld);
            return new WorldServer[] { defaultWorld, netherWorld, endWorld };
        }

        LOGGER.info("Returning null as there are no default worlds");
        // Returning null will just run the original load world method
        return null;
    }

    @Override
    public boolean isCustomWorld(Object world) {
        return world instanceof com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer;
    }

    @Override
    public boolean skipWorldAdd(Object world) {
        if (!isCustomWorld(world) || nmsInstance.isLoadingDefaultWorlds()) {
            return false;
        }

        com.grinderwolf.swm.nms.v1_16_R2.CustomWorldServer worldServer = (CustomWorldServer) world;
        return !worldServer.isReady();
    }

    static void initialize(v1_16_R2SlimeNMS instance) {
        LOGGER.info("registering CLSM bridge");

        ClassModifier.setLoader(new com.grinderwolf.swm.nms.v1_16_R2.CraftCLSMBridge(instance));
    }
}
