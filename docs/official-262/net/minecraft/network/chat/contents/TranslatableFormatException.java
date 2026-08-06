/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.chat.contents;

import java.util.Locale;
import net.minecraft.network.chat.contents.TranslatableContents;

public class TranslatableFormatException
extends IllegalArgumentException {
    public TranslatableFormatException(TranslatableContents component, String message) {
        super(String.format(Locale.ROOT, "Error parsing: %s: %s", component, message));
    }

    public TranslatableFormatException(TranslatableContents component, int index) {
        super(String.format(Locale.ROOT, "Invalid index %d requested for %s", index, component));
    }

    public TranslatableFormatException(TranslatableContents component, Throwable t) {
        super(String.format(Locale.ROOT, "Error while parsing: %s", component), t);
    }
}

