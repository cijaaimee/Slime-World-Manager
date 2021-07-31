package com.grinderwolf.swm.nms.v11645;

import com.grinderwolf.swm.clsm.CLSMBridge;
import com.grinderwolf.swm.clsm.ClassModifier;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.DedicatedServer;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.ProtoChunkExtension;
import net.minecraft.server.v1_16_R3.WorldServer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CraftCLSMBridge implements CLSMBridge {

    private final v11645SlimeNMS nmsInstance;

    @Override
    public Object getChunk(Object worldObject, int x, int z) {
        CustomWorldServer world = (CustomWorldServer) worldObject;
        return world.getChunk(x, z);
    }

    @Override
    public boolean saveChunk(Object world, Object chunkAccess) {
        if (!(world instanceof CustomWorldServer)) {
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

        ((CustomWorldServer) world).saveChunk(chunk);
        chunk.setNeedsSaving(false);

        return true;
    }

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

    @Override
    public Object getDefaultGamemode() {
        if (nmsInstance.isLoadingDefaultWorlds()) {
            return ((DedicatedServer) MinecraftServer.getServer()).getDedicatedServerProperties().gamemode;
        }

        return null;
    }

    static void initialize(v11645SlimeNMS instance) {
        ClassModifier.setLoader(new CraftCLSMBridge(instance));
    }
}
