package com.grinderwolf.swm.nms.world;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeWorld;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface SlimeLoadedWorld extends SlimeWorld {

    byte getVersion();

    void updateVersion(byte version);

    void updateChunk(SlimeChunk chunk);

    CompletableFuture<byte[]> serialize() throws IOException;

    void setLoader(SlimeLoader newLoader);
}
