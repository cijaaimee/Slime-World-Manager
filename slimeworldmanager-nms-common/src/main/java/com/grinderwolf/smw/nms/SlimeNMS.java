package com.grinderwolf.smw.nms;

import com.grinderwolf.smw.api.SlimeWorld;

public interface SlimeNMS {

    public void inject();
    public void loadWorld(SlimeWorld world);
    public void unloadWorld(SlimeWorld world);
}
