/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringEscapeUtils
 */
package net.minecraft;

import org.apache.commons.lang3.StringEscapeUtils;

public class IdentifierException
extends RuntimeException {
    public IdentifierException(String message) {
        super(StringEscapeUtils.escapeJava((String)message));
    }

    public IdentifierException(String message, Throwable cause) {
        super(StringEscapeUtils.escapeJava((String)message), cause);
    }
}

