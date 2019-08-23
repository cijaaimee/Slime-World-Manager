package com.grinderwolf.swm.api.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a 1.13 or newer world is loaded
 * on a 1.12.2 or older server and vice versa.
 */
public class UnsupportedWorldException extends SlimeException {

    @Getter
    private final byte worldVersion;

    public UnsupportedWorldException(String world, byte worldVersion, byte nmsVersion) {
        super("world " + world + " is a  v" + worldVersion + " world, while the server only supports v" + nmsVersion);
        this.worldVersion = worldVersion;
    }
}
