package com.grinderwolf.swm.api.exceptions;

/**
 * Exception thrown when a
 * world could not be found.
 */
public class WorldAlreadyExistsException extends SlimeException {

    public WorldAlreadyExistsException(String world) {
        super("World " + world + " already exists!");
    }
}
