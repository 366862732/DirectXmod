/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import net.minecraft.network.chat.Style;

@FunctionalInterface
public interface FormattedCharSink {
    public boolean accept(int var1, Style var2, int var3);
}

