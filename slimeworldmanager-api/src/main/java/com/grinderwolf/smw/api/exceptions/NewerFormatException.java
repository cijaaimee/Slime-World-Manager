package com.grinderwolf.smw.api.exceptions;

/**
 * Exception thrown when a world is encoded
 * using a newer SRF format than the one that
 * SMW supports.
 */
public class NewerFormatException extends SlimeException {

    public NewerFormatException(byte version) {
        super("v" + version);
    }
}
