/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

/** Exception thrown when a world is loaded when trying to import it. */
public class WorldLoadedException extends SlimeException {

    public WorldLoadedException(String worldName) {
        super("World " + worldName + " is loaded! Unload it before importing it.");
    }
}
