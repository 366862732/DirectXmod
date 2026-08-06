/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonPrimitive
 *  com.google.gson.JsonSyntaxException
 *  com.google.gson.Strictness
 *  com.google.gson.internal.Streams
 *  com.google.gson.reflect.TypeToken
 *  com.google.gson.stream.JsonReader
 *  com.google.gson.stream.JsonWriter
 *  org.apache.commons.lang3.StringUtils
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class GsonHelper {
    private static final Gson GSON = new GsonBuilder().create();

    public static boolean isStringValue(JsonObject node, String name) {
        if (!GsonHelper.isValidPrimitive(node, name)) {
            return false;
        }
        return node.getAsJsonPrimitive(name).isString();
    }

    public static boolean isStringValue(JsonElement node) {
        if (!node.isJsonPrimitive()) {
            return false;
        }
        return node.getAsJsonPrimitive().isString();
    }

    public static boolean isNumberValue(JsonObject node, String name) {
        if (!GsonHelper.isValidPrimitive(node, name)) {
            return false;
        }
        return node.getAsJsonPrimitive(name).isNumber();
    }

    public static boolean isNumberValue(JsonElement node) {
        if (!node.isJsonPrimitive()) {
            return false;
        }
        return node.getAsJsonPrimitive().isNumber();
    }

    public static boolean isBooleanValue(JsonObject node, String name) {
        if (!GsonHelper.isValidPrimitive(node, name)) {
            return false;
        }
        return node.getAsJsonPrimitive(name).isBoolean();
    }

    public static boolean isBooleanValue(JsonElement node) {
        if (!node.isJsonPrimitive()) {
            return false;
        }
        return node.getAsJsonPrimitive().isBoolean();
    }

    public static boolean isArrayNode(JsonObject node, String name) {
        if (!GsonHelper.isValidNode(node, name)) {
            return false;
        }
        return node.get(name).isJsonArray();
    }

    public static boolean isObjectNode(JsonObject node, String name) {
        if (!GsonHelper.isValidNode(node, name)) {
            return false;
        }
        return node.get(name).isJsonObject();
    }

    public static boolean isValidPrimitive(JsonObject node, String name) {
        if (!GsonHelper.isValidNode(node, name)) {
            return false;
        }
        return node.get(name).isJsonPrimitive();
    }

    public static boolean isValidNode(@Nullable JsonObject node, String name) {
        if (node == null) {
            return false;
        }
        return node.get(name) != null;
    }

    public static JsonElement getNonNull(JsonObject object, String name) {
        JsonElement result = object.get(name);
        if (result == null || result.isJsonNull()) {
            throw new JsonSyntaxException("Missing field " + name);
        }
        return result;
    }

    public static String convertToString(JsonElement element, String name) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a string, was " + GsonHelper.getType(element));
    }

    public static String getAsString(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToString(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a string");
    }

    @Contract(value="_,_,!null->!null;_,_,null->_")
    public static @Nullable String getAsString(JsonObject object, String name, @Nullable String def) {
        if (object.has(name)) {
            return GsonHelper.convertToString(object.get(name), name);
        }
        return def;
    }

    public static Holder<Item> convertToItem(JsonElement element, String name) {
        if (element.isJsonPrimitive()) {
            String itemName = element.getAsString();
            return BuiltInRegistries.ITEM.get(Identifier.parse(itemName)).orElseThrow(() -> new JsonSyntaxException("Expected " + name + " to be an item, was unknown string '" + itemName + "'"));
        }
        throw new JsonSyntaxException("Expected " + name + " to be an item, was " + GsonHelper.getType(element));
    }

    public static Holder<Item> getAsItem(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToItem(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find an item");
    }

    @Contract(value="_,_,!null->!null;_,_,null->_")
    public static @Nullable Holder<Item> getAsItem(JsonObject object, String name, @Nullable Holder<Item> def) {
        if (object.has(name)) {
            return GsonHelper.convertToItem(object.get(name), name);
        }
        return def;
    }

    public static boolean convertToBoolean(JsonElement element, String name) {
        if (element.isJsonPrimitive()) {
            return element.getAsBoolean();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Boolean, was " + GsonHelper.getType(element));
    }

    public static boolean getAsBoolean(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToBoolean(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Boolean");
    }

    public static boolean getAsBoolean(JsonObject object, String name, boolean def) {
        if (object.has(name)) {
            return GsonHelper.convertToBoolean(object.get(name), name);
        }
        return def;
    }

    public static double convertToDouble(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsDouble();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Double, was " + GsonHelper.getType(element));
    }

    public static double getAsDouble(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToDouble(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Double");
    }

    public static double getAsDouble(JsonObject object, String name, double def) {
        if (object.has(name)) {
            return GsonHelper.convertToDouble(object.get(name), name);
        }
        return def;
    }

    public static float convertToFloat(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsFloat();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Float, was " + GsonHelper.getType(element));
    }

    public static float getAsFloat(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToFloat(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Float");
    }

    public static float getAsFloat(JsonObject object, String name, float def) {
        if (object.has(name)) {
            return GsonHelper.convertToFloat(object.get(name), name);
        }
        return def;
    }

    public static long convertToLong(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsLong();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Long, was " + GsonHelper.getType(element));
    }

    public static long getAsLong(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToLong(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Long");
    }

    public static long getAsLong(JsonObject object, String name, long def) {
        if (object.has(name)) {
            return GsonHelper.convertToLong(object.get(name), name);
        }
        return def;
    }

    public static int convertToInt(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Int, was " + GsonHelper.getType(element));
    }

    public static int getAsInt(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToInt(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Int");
    }

    public static int getAsInt(JsonObject object, String name, int def) {
        if (object.has(name)) {
            return GsonHelper.convertToInt(object.get(name), name);
        }
        return def;
    }

    public static byte convertToByte(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsByte();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Byte, was " + GsonHelper.getType(element));
    }

    public static byte getAsByte(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToByte(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Byte");
    }

    public static byte getAsByte(JsonObject object, String name, byte def) {
        if (object.has(name)) {
            return GsonHelper.convertToByte(object.get(name), name);
        }
        return def;
    }

    public static char convertToCharacter(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsCharacter();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Character, was " + GsonHelper.getType(element));
    }

    public static char getAsCharacter(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToCharacter(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Character");
    }

    public static char getAsCharacter(JsonObject object, String name, char def) {
        if (object.has(name)) {
            return GsonHelper.convertToCharacter(object.get(name), name);
        }
        return def;
    }

    public static BigDecimal convertToBigDecimal(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsBigDecimal();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a BigDecimal, was " + GsonHelper.getType(element));
    }

    public static BigDecimal getAsBigDecimal(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToBigDecimal(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a BigDecimal");
    }

    public static BigDecimal getAsBigDecimal(JsonObject object, String name, BigDecimal def) {
        if (object.has(name)) {
            return GsonHelper.convertToBigDecimal(object.get(name), name);
        }
        return def;
    }

    public static BigInteger convertToBigInteger(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsBigInteger();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a BigInteger, was " + GsonHelper.getType(element));
    }

    public static BigInteger getAsBigInteger(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToBigInteger(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a BigInteger");
    }

    public static BigInteger getAsBigInteger(JsonObject object, String name, BigInteger def) {
        if (object.has(name)) {
            return GsonHelper.convertToBigInteger(object.get(name), name);
        }
        return def;
    }

    public static short convertToShort(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsShort();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a Short, was " + GsonHelper.getType(element));
    }

    public static short getAsShort(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToShort(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a Short");
    }

    public static short getAsShort(JsonObject object, String name, short def) {
        if (object.has(name)) {
            return GsonHelper.convertToShort(object.get(name), name);
        }
        return def;
    }

    public static JsonObject convertToJsonObject(JsonElement element, String name) {
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a JsonObject, was " + GsonHelper.getType(element));
    }

    public static JsonObject getAsJsonObject(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToJsonObject(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonObject");
    }

    @Contract(value="_,_,!null->!null;_,_,null->_")
    public static @Nullable JsonObject getAsJsonObject(JsonObject object, String name, @Nullable JsonObject def) {
        if (object.has(name)) {
            return GsonHelper.convertToJsonObject(object.get(name), name);
        }
        return def;
    }

    public static JsonArray convertToJsonArray(JsonElement element, String name) {
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a JsonArray, was " + GsonHelper.getType(element));
    }

    public static JsonArray getAsJsonArray(JsonObject object, String name) {
        if (object.has(name)) {
            return GsonHelper.convertToJsonArray(object.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonArray");
    }

    @Contract(value="_,_,!null->!null;_,_,null->_")
    public static @Nullable JsonArray getAsJsonArray(JsonObject object, String name, @Nullable JsonArray def) {
        if (object.has(name)) {
            return GsonHelper.convertToJsonArray(object.get(name), name);
        }
        return def;
    }

    public static <T> T convertToObject(@Nullable JsonElement element, String name, JsonDeserializationContext context, Class<? extends T> clazz) {
        if (element != null) {
            return (T)context.deserialize(element, clazz);
        }
        throw new JsonSyntaxException("Missing " + name);
    }

    public static <T> T getAsObject(JsonObject object, String name, JsonDeserializationContext context, Class<? extends T> clazz) {
        if (object.has(name)) {
            return GsonHelper.convertToObject(object.get(name), name, context, clazz);
        }
        throw new JsonSyntaxException("Missing " + name);
    }

    @Contract(value="_,_,!null,_,_->!null;_,_,null,_,_->_")
    public static <T> @Nullable T getAsObject(JsonObject object, String name, @Nullable T def, JsonDeserializationContext context, Class<? extends T> clazz) {
        if (object.has(name)) {
            return GsonHelper.convertToObject(object.get(name), name, context, clazz);
        }
        return def;
    }

    public static String getType(@Nullable JsonElement element) {
        String value = StringUtils.abbreviateMiddle((String)String.valueOf(element), (String)"...", (int)10);
        if (element == null) {
            return "null (missing)";
        }
        if (element.isJsonNull()) {
            return "null (json)";
        }
        if (element.isJsonArray()) {
            return "an array (" + value + ")";
        }
        if (element.isJsonObject()) {
            return "an object (" + value + ")";
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return "a number (" + value + ")";
            }
            if (primitive.isBoolean()) {
                return "a boolean (" + value + ")";
            }
        }
        return value;
    }

    public static <T> T fromJson(Gson gson, Reader reader, Class<T> type) {
        try {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setStrictness(Strictness.STRICT);
            Object result = gson.getAdapter(type).read(jsonReader);
            if (result == null) {
                throw new JsonParseException("JSON data was null or empty");
            }
            return (T)result;
        }
        catch (IOException e) {
            throw new JsonParseException((Throwable)e);
        }
    }

    public static <T> @Nullable T fromNullableJson(Gson gson, Reader reader, TypeToken<T> type) {
        try {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setStrictness(Strictness.STRICT);
            return (T)gson.getAdapter(type).read(jsonReader);
        }
        catch (IOException e) {
            throw new JsonParseException((Throwable)e);
        }
    }

    public static <T> T fromJson(Gson gson, Reader reader, TypeToken<T> type) {
        T result = GsonHelper.fromNullableJson(gson, reader, type);
        if (result == null) {
            throw new JsonParseException("JSON data was null or empty");
        }
        return result;
    }

    public static <T> @Nullable T fromNullableJson(Gson gson, String string, TypeToken<T> type) {
        return GsonHelper.fromNullableJson(gson, new StringReader(string), type);
    }

    public static <T> T fromJson(Gson gson, String string, Class<T> type) {
        return GsonHelper.fromJson(gson, (Reader)new StringReader(string), type);
    }

    public static JsonObject parse(String string) {
        return GsonHelper.parse(new StringReader(string));
    }

    public static JsonObject parse(Reader reader) {
        return GsonHelper.fromJson(GSON, reader, JsonObject.class);
    }

    public static JsonArray parseArray(String string) {
        return GsonHelper.parseArray(new StringReader(string));
    }

    public static JsonArray parseArray(Reader reader) {
        return GsonHelper.fromJson(GSON, reader, JsonArray.class);
    }

    public static String toStableString(JsonElement jsonElement) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter((Writer)stringWriter);
        try {
            GsonHelper.writeValue(jsonWriter, jsonElement, Comparator.naturalOrder());
        }
        catch (IOException e) {
            throw new AssertionError((Object)e);
        }
        return stringWriter.toString();
    }

    public static void writeValue(JsonWriter out, @Nullable JsonElement value, @Nullable Comparator<String> keyComparator) throws IOException {
        if (value == null || value.isJsonNull()) {
            out.nullValue();
        } else if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                out.value(primitive.getAsNumber());
            } else if (primitive.isBoolean()) {
                out.value(primitive.getAsBoolean());
            } else {
                out.value(primitive.getAsString());
            }
        } else if (value.isJsonArray()) {
            out.beginArray();
            for (JsonElement e : value.getAsJsonArray()) {
                GsonHelper.writeValue(out, e, keyComparator);
            }
            out.endArray();
        } else if (value.isJsonObject()) {
            out.beginObject();
            for (Map.Entry<String, JsonElement> e : GsonHelper.sortByKeyIfNeeded(value.getAsJsonObject().entrySet(), keyComparator)) {
                out.name(e.getKey());
                GsonHelper.writeValue(out, e.getValue(), keyComparator);
            }
            out.endObject();
        } else {
            throw new IllegalArgumentException("Couldn't write " + String.valueOf(value.getClass()));
        }
    }

    private static Collection<Map.Entry<String, JsonElement>> sortByKeyIfNeeded(Collection<Map.Entry<String, JsonElement>> elements, @Nullable Comparator<String> keyComparator) {
        if (keyComparator == null) {
            return elements;
        }
        ArrayList<Map.Entry<String, JsonElement>> sorted = new ArrayList<Map.Entry<String, JsonElement>>(elements);
        sorted.sort(Map.Entry.comparingByKey(keyComparator));
        return sorted;
    }

    public static boolean encodesLongerThan(JsonElement element, int limit) {
        try {
            Streams.write((JsonElement)element, (JsonWriter)new JsonWriter(Streams.writerForAppendable((Appendable)new CountedAppendable(limit))));
        }
        catch (IllegalStateException e) {
            return true;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return false;
    }

    private static class CountedAppendable
    implements Appendable {
        private int totalCount;
        private final int limit;

        public CountedAppendable(int limit) {
            this.limit = limit;
        }

        private Appendable accountChars(int count) {
            this.totalCount += count;
            if (this.totalCount > this.limit) {
                throw new IllegalStateException("Character count over limit: " + this.totalCount + " > " + this.limit);
            }
            return this;
        }

        @Override
        public Appendable append(CharSequence csq) {
            return this.accountChars(csq.length());
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            return this.accountChars(end - start);
        }

        @Override
        public Appendable append(char c) {
            return this.accountChars(1);
        }
    }
}

