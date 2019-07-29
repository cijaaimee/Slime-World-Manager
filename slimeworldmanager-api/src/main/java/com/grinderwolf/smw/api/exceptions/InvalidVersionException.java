package com.grinderwolf.smw.api.exceptions;

/**
 * Exception thrown when SMW is loaded
 * on a non-supported Spigot version.
 */
public class InvalidVersionException extends SlimeException {

    public InvalidVersionException(String version) {
        super("SlimeWorldManager does not support Spigot v" + version + "!");
    }
}
