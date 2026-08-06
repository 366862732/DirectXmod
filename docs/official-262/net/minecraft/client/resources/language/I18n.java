/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.language;

import java.util.IllegalFormatException;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.locale.Language;

@Environment(value=EnvType.CLIENT)
public class I18n {
    private I18n() {
    }

    public static String get(String id, Object ... args) {
        String value = Language.getInstance().getOrDefault(id);
        try {
            return String.format(Locale.ROOT, value, args);
        }
        catch (IllegalFormatException ignored) {
            return "Format error: " + value;
        }
    }
}

