/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

/** Exception thrown when a MC world is too big to be converted into the SRF. */
public class WorldTooBigException extends SlimeException {

    public WorldTooBigException(String worldName) {
        super("World " + worldName + " is too big to be converted into the SRF!");
    }
}
