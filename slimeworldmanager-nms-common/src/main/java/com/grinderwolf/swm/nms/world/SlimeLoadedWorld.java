package com.grinderwolf.swm.nms.world;

import com.grinderwolf.swm.api.loaders.*;
import com.grinderwolf.swm.api.world.*;
import com.grinderwolf.swm.nms.*;

import java.io.*;
import java.util.concurrent.*;

public interface SlimeLoadedWorld extends SlimeWorld {

    byte getVersion();

    void updateVersion(byte version);

    void updateChunk(SlimeChunk chunk);

    CompletableFuture<byte[]> serialize() throws IOException;

    void setLoader(SlimeLoader newLoader);
}
