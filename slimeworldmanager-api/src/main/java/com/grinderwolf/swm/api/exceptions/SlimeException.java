/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.exceptions;

/** Generic SWM exception. */
public class SlimeException extends Exception {

    public SlimeException(String message) {
        super(message);
    }

    public SlimeException(String message, Exception ex) {
        super(message, ex);
    }
}
