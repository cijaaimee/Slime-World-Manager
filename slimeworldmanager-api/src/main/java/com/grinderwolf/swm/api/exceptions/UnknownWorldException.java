/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

/** Exception thrown when a world could not be found. */
public class UnknownWorldException extends SlimeException {

    public UnknownWorldException(String world) {
        super("Unknown world " + world);
    }
}
