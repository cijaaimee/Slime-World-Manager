package com.grinderwolf.smw.api.exceptions;

public class WorldInUseException extends SlimeException {

    public WorldInUseException(String world) {
        super(world);
    }
}
