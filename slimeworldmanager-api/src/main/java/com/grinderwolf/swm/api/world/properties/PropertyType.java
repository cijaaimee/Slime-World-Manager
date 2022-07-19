/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.world.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Enum containing all the types of properties. */
@Getter
@RequiredArgsConstructor
public enum PropertyType {
    STRING(String.class),
    BOOLEAN(Boolean.class),
    INT(Integer.class);

    private final Class<?> valueClazz;
}
