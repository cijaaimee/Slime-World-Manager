/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.plugin.world.importer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class LevelData {

    private final int version;
    private final Map<String, String> gameRules;

    private final int spawnX;
    private final int spawnY;
    private final int spawnZ;
}
