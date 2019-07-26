package com.grinderwolf.smw.api.exceptions;

public class CorruptedWorldException extends SlimeException {

    public CorruptedWorldException(String world) {
        super("World " + world + " seems to be corrupted");
    }
}
