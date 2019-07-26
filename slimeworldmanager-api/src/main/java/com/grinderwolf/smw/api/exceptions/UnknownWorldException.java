package com.grinderwolf.smw.api.exceptions;

public class UnknownWorldException extends SlimeException {

    public UnknownWorldException(String world) {
        super("Unknown world " + world);
    }
}
