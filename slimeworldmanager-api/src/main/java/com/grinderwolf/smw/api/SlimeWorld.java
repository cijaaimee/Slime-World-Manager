package com.grinderwolf.smw.api;

public interface SlimeWorld {

    public String getName();
    public SlimeLoader getLoader();
    public SlimeChunk getChunk(int x, int z);
    public void updateChunk(SlimeChunk chunk);
}
