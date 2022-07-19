/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

/** Exception thrown when a world is locked and is being accessed on write-mode. */
public class WorldInUseException extends SlimeException {

    public WorldInUseException(String world) {
        super(world);
    }
}
