/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StrictJsonParser;

public class LegacyComponentDataFixUtils {
    private static final String EMPTY_CONTENTS = LegacyComponentDataFixUtils.createTextComponentJson("");

    public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> ops, String text) {
        String stableString = LegacyComponentDataFixUtils.createTextComponentJson(text);
        return new Dynamic(ops, ops.createString(stableString));
    }

    public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> ops) {
        return new Dynamic(ops, ops.createString(EMPTY_CONTENTS));
    }

    public static String createTextComponentJson(String text) {
        JsonObject result = new JsonObject();
        result.addProperty("text", text);
        return GsonHelper.toStableString((JsonElement)result);
    }

    public static String createTranslatableComponentJson(String key) {
        JsonObject result = new JsonObject();
        result.addProperty("translate", key);
        return GsonHelper.toStableString((JsonElement)result);
    }

    public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> ops, String key) {
        String stableString = LegacyComponentDataFixUtils.createTranslatableComponentJson(key);
        return new Dynamic(ops, ops.createString(stableString));
    }

    public static String rewriteFromLenient(String string) {
        if (string.isEmpty() || string.equals("null")) {
            return EMPTY_CONTENTS;
        }
        char firstChar = string.charAt(0);
        char lastChar = string.charAt(string.length() - 1);
        if (firstChar == '\"' && lastChar == '\"' || firstChar == '{' && lastChar == '}' || firstChar == '[' && lastChar == ']') {
            try {
                JsonElement json = LenientJsonParser.parse(string);
                if (json.isJsonPrimitive()) {
                    return LegacyComponentDataFixUtils.createTextComponentJson(json.getAsString());
                }
                return GsonHelper.toStableString(json);
            }
            catch (JsonParseException jsonParseException) {
                // empty catch block
            }
        }
        return LegacyComponentDataFixUtils.createTextComponentJson(string);
    }

    public static boolean isStrictlyValidJson(Dynamic<?> component) {
        return component.asString().result().filter(string -> {
            try {
                StrictJsonParser.parse(string);
                return true;
            }
            catch (JsonParseException ignored) {
                return false;
            }
        }).isPresent();
    }

    public static Optional<String> extractTranslationString(String component) {
        try {
            JsonObject parsedObject;
            JsonElement key;
            JsonElement parsed = LenientJsonParser.parse(component);
            if (parsed.isJsonObject() && (key = (parsedObject = parsed.getAsJsonObject()).get("translate")) != null && key.isJsonPrimitive()) {
                return Optional.of(key.getAsString());
            }
        }
        catch (JsonParseException jsonParseException) {
            // empty catch block
        }
        return Optional.empty();
    }
}

