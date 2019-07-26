package com.grinderwolf.smw.nms;

import com.grinderwolf.smw.api.SlimeChunkSection;
import com.grinderwolf.smw.api.utils.NibbleArray;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CraftSlimeChunkSection implements SlimeChunkSection {

    private final byte[] blocks;
    private final NibbleArray data;
    private final NibbleArray blockLight;
    private final NibbleArray skyLight;
}
