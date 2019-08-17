package com.grinderwolf.swm.api.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a 1.13 or newer world is loaded
 * on a 1.12.2 or older server and vice versa.
 */
public class UnsupportedWorldException extends SlimeException {

    @Getter
    private final boolean v1_13;

    public UnsupportedWorldException(String world, boolean v1_13) {
        super("World " + world + " is meant to be loaded on a " + (v1_13 ? "1.13 or newer" : "1.12 or older") + " server");
        this.v1_13 = v1_13;
    }
}
