package com.grinderwolf.smw.api.exceptions;


public class InvalidVersionException extends SlimeException {

    public InvalidVersionException(String version) {
        super("SlimeWorldManager does not support Spigot v" + version + "!");
    }
}
