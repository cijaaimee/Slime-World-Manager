package com.grinderwolf.swm.api.exceptions;

/**
 * Exception thrown when a world could not
 * be read from its data file.
 */
public class CorruptedWorldException extends SlimeException {

    public CorruptedWorldException(String world) {
        super("World " + world + " seems to be corrupted");
    }
}
