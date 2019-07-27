package com.grinderwolf.smw.api.exceptions;

public class NewerFormatException extends SlimeException {

    public NewerFormatException(byte version) {
        super("v" + version);
    }
}
