/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

import java.io.File;

/** Exception thrown when a folder does not contain a valid Minecraft world. */
public class InvalidWorldException extends SlimeException {

    public InvalidWorldException(File worldDir) {
        super("Directory " + worldDir.getPath() + " does not contain a valid MC world!");
    }
}
