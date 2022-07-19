/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

/**
 * Exception thrown when a world is encoded using a newer SRF format than the one that SWM supports.
 */
public class NewerFormatException extends SlimeException {

    public NewerFormatException(byte version) {
        super("v" + version);
    }
}
