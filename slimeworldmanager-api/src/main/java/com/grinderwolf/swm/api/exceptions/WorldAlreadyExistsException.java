/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

/** Exception thrown when a world already exists inside a data source. */
public class WorldAlreadyExistsException extends SlimeException {

    public WorldAlreadyExistsException(String world) {
        super("World " + world + " already exists!");
    }
}
