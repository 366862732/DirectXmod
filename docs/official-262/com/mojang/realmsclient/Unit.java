/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public enum Unit {
    B,
    KB,
    MB,
    GB;

    private static final int BASE_UNIT = 1024;

    public static Unit getLargest(long bytes) {
        if (bytes < 1024L) {
            return B;
        }
        try {
            int exp = (int)(Math.log(bytes) / Math.log(1024.0));
            String pre = String.valueOf("KMGTPE".charAt(exp - 1));
            return Unit.valueOf(pre + "B");
        }
        catch (Exception ignored) {
            return GB;
        }
    }

    public static double convertTo(long bytes, Unit unit) {
        if (unit == B) {
            return bytes;
        }
        return (double)bytes / Math.pow(1024.0, unit.ordinal());
    }

    public static String humanReadable(long bytes) {
        int unit = 1024;
        if (bytes < 1024L) {
            return bytes + " B";
        }
        int exp = (int)(Math.log(bytes) / Math.log(1024.0));
        String pre = "" + "KMGTPE".charAt(exp - 1);
        return String.format(Locale.ROOT, "%.1f %sB", (double)bytes / Math.pow(1024.0, exp), pre);
    }

    public static String humanReadable(long bytes, Unit unit) {
        return String.format(Locale.ROOT, "%." + (unit == GB ? "1" : "0") + "f %s", Unit.convertTo(bytes, unit), unit.name());
    }
}

