/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.api.utils;

/** Class containing some standards of the SRF. */
public class SlimeFormat {

    /** First bytes of every SRF file * */
    public static final byte[] SLIME_HEADER = new byte[] {-79, 11};

    /** Latest version of the SRF that SWM supports * */
    public static final byte SLIME_VERSION = 9;
}
