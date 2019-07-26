package com.grinderwolf.smw.nms.v1_8_R3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.WorldData;

@Getter
@RequiredArgsConstructor
public class CustomWorldData extends WorldData {

    private final String name;
}
