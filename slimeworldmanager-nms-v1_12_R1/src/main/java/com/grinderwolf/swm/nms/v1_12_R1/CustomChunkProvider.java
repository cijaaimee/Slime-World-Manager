package com.grinderwolf.swm.nms.v1_12_R1;

import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_12_R1.ChunkProviderServer;
import net.minecraft.server.v1_12_R1.CrashReport;
import net.minecraft.server.v1_12_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_12_R1.ReportedException;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_12_R1.generator.NormalChunkGenerator;

public class CustomChunkProvider extends ChunkProviderServer {

    public CustomChunkProvider(CustomWorldServer server) {
        super(server, new CustomChunkLoader(server.getSlimeWorld()), new NormalChunkGenerator(server, server.getSeed()));
    }

    @Override
    public Chunk originalGetChunkAt(int xPos, int zPos) {
        Chunk chunk = this.getLoadedChunkAt(xPos, zPos);
        boolean newChunk = false;

        if (chunk == null) {
            world.timings.syncChunkLoadTimer.startTiming();
            chunk = this.loadChunk(xPos, zPos);
            long k = ChunkCoordIntPair.a(xPos, zPos);

            if (chunk == null) {
                try {
                    chunk = this.chunkGenerator.getOrCreateChunk(xPos, zPos);
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.a(throwable, "Exception generating new chunk");
                    CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Chunk to be generated");

                    crashreportsystemdetails.a("Location", String.format("%d,%d", xPos, zPos));
                    crashreportsystemdetails.a("Position hash", k);
                    crashreportsystemdetails.a("Generator", this.chunkGenerator);
                    throw new ReportedException(crashreport);
                }

                newChunk = true;
            }

            this.chunks.put(k, chunk);
            chunk.addEntities();

            Server server = world.getServer();

            if (server != null) {
                server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk.bukkitChunk, newChunk));
            }

            // Update neighbor counts
            for (int x = -2; x < 3; x++) {
                for (int z = -2; z < 3; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }

                    Chunk neighbor = this.getChunkIfLoaded(chunk.locX + x, chunk.locZ + z);
                    if (neighbor != null) {
                        neighbor.setNeighborLoaded(-x, -z);
                        chunk.setNeighborLoaded(x, z);
                    }
                }
            }

            chunk.loadNearby(this, this.chunkGenerator, newChunk);
            world.timings.syncChunkLoadTimer.stopTiming();
        }

        return chunk;
    }
}
