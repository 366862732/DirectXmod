/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParticleUnflatteningFix
extends DataFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public ParticleUnflatteningFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type oldType = this.getInputSchema().getType(References.PARTICLE);
        Type newType = this.getOutputSchema().getType(References.PARTICLE);
        return this.writeFixAndRead("ParticleUnflatteningFix", oldType, newType, this::fix);
    }

    private <T> Dynamic<T> fix(Dynamic<T> input) {
        Optional maybeString = input.asString().result();
        if (maybeString.isEmpty()) {
            return input;
        }
        String particleDescription = (String)maybeString.get();
        String[] parts = particleDescription.split(" ", 2);
        String id = NamespacedSchema.ensureNamespaced(parts[0]);
        Dynamic<T> result = input.createMap(Map.of(input.createString("type"), input.createString(id)));
        return switch (id) {
            case "minecraft:item" -> {
                if (parts.length > 1) {
                    yield this.updateItem(result, parts[1]);
                }
                yield result;
            }
            case "minecraft:block", "minecraft:block_marker", "minecraft:falling_dust", "minecraft:dust_pillar" -> {
                if (parts.length > 1) {
                    yield this.updateBlock(result, parts[1]);
                }
                yield result;
            }
            case "minecraft:dust" -> {
                if (parts.length > 1) {
                    yield this.updateDust(result, parts[1]);
                }
                yield result;
            }
            case "minecraft:dust_color_transition" -> {
                if (parts.length > 1) {
                    yield this.updateDustTransition(result, parts[1]);
                }
                yield result;
            }
            case "minecraft:sculk_charge" -> {
                if (parts.length > 1) {
                    yield this.updateSculkCharge(result, parts[1]);
                }
                yield result;
            }
            case "minecraft:vibration" -> {
                if (parts.length > 1) {
                    yield this.updateVibration(result, parts[1]);
                }
                yield result;
            }
            case "minecraft:shriek" -> {
                if (parts.length > 1) {
                    yield this.updateShriek(result, parts[1]);
                }
                yield result;
            }
            default -> result;
        };
    }

    private <T> Dynamic<T> updateItem(Dynamic<T> result, String contents) {
        int tagPartStart = contents.indexOf("{");
        Dynamic itemStack = result.createMap(Map.of(result.createString("Count"), result.createInt(1)));
        if (tagPartStart == -1) {
            itemStack = itemStack.set("id", result.createString(contents));
        } else {
            itemStack = itemStack.set("id", result.createString(contents.substring(0, tagPartStart)));
            Dynamic<T> itemTag = ParticleUnflatteningFix.parseTag(result.getOps(), contents.substring(tagPartStart));
            if (itemTag != null) {
                itemStack = itemStack.set("tag", itemTag);
            }
        }
        return result.set("item", itemStack);
    }

    private static <T> @Nullable Dynamic<T> parseTag(DynamicOps<T> ops, String contents) {
        try {
            return new Dynamic(ops, TagParser.create(ops).parseFully(contents));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse tag: {}", (Object)contents, (Object)e);
            return null;
        }
    }

    private <T> Dynamic<T> updateBlock(Dynamic<T> result, String contents) {
        int statePartStart = contents.indexOf("[");
        Dynamic blockState = result.emptyMap();
        if (statePartStart == -1) {
            blockState = blockState.set("Name", result.createString(NamespacedSchema.ensureNamespaced(contents)));
        } else {
            blockState = blockState.set("Name", result.createString(NamespacedSchema.ensureNamespaced(contents.substring(0, statePartStart))));
            Map<Dynamic<T>, Dynamic<T>> properties = ParticleUnflatteningFix.parseBlockProperties(result, contents.substring(statePartStart));
            if (!properties.isEmpty()) {
                blockState = blockState.set("Properties", result.createMap(properties));
            }
        }
        return result.set("block_state", blockState);
    }

    private static <T> Map<Dynamic<T>, Dynamic<T>> parseBlockProperties(Dynamic<T> dynamic, String contents) {
        try {
            HashMap<Dynamic<T>, Dynamic<T>> result = new HashMap<Dynamic<T>, Dynamic<T>>();
            StringReader reader = new StringReader(contents);
            reader.expect('[');
            reader.skipWhitespace();
            while (reader.canRead() && reader.peek() != ']') {
                reader.skipWhitespace();
                String key = reader.readString();
                reader.skipWhitespace();
                reader.expect('=');
                reader.skipWhitespace();
                String value = reader.readString();
                reader.skipWhitespace();
                result.put(dynamic.createString(key), dynamic.createString(value));
                if (!reader.canRead()) continue;
                if (reader.peek() != ',') break;
                reader.skip();
            }
            reader.expect(']');
            return result;
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse block properties: {}", (Object)contents, (Object)e);
            return Map.of();
        }
    }

    private static <T> Dynamic<T> readVector(Dynamic<T> result, StringReader reader) throws CommandSyntaxException {
        float x = reader.readFloat();
        reader.expect(' ');
        float y = reader.readFloat();
        reader.expect(' ');
        float z = reader.readFloat();
        return result.createList(Stream.of(Float.valueOf(x), Float.valueOf(y), Float.valueOf(z)).map(arg_0 -> result.createFloat(arg_0)));
    }

    private <T> Dynamic<T> updateDust(Dynamic<T> result, String contents) {
        try {
            StringReader reader = new StringReader(contents);
            Dynamic<T> vector = ParticleUnflatteningFix.readVector(result, reader);
            reader.expect(' ');
            float scale = reader.readFloat();
            return result.set("color", vector).set("scale", result.createFloat(scale));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse particle options: {}", (Object)contents, (Object)e);
            return result;
        }
    }

    private <T> Dynamic<T> updateDustTransition(Dynamic<T> result, String contents) {
        try {
            StringReader reader = new StringReader(contents);
            Dynamic<T> from = ParticleUnflatteningFix.readVector(result, reader);
            reader.expect(' ');
            float scale = reader.readFloat();
            reader.expect(' ');
            Dynamic<T> to = ParticleUnflatteningFix.readVector(result, reader);
            return result.set("from_color", from).set("to_color", to).set("scale", result.createFloat(scale));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse particle options: {}", (Object)contents, (Object)e);
            return result;
        }
    }

    private <T> Dynamic<T> updateSculkCharge(Dynamic<T> result, String contents) {
        try {
            StringReader reader = new StringReader(contents);
            float roll = reader.readFloat();
            return result.set("roll", result.createFloat(roll));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse particle options: {}", (Object)contents, (Object)e);
            return result;
        }
    }

    private <T> Dynamic<T> updateVibration(Dynamic<T> result, String contents) {
        try {
            StringReader reader = new StringReader(contents);
            float destX = (float)reader.readDouble();
            reader.expect(' ');
            float destY = (float)reader.readDouble();
            reader.expect(' ');
            float destZ = (float)reader.readDouble();
            reader.expect(' ');
            int arrivalInTicks = reader.readInt();
            Dynamic blockPos = result.createIntList(IntStream.of(Mth.floor(destX), Mth.floor(destY), Mth.floor(destZ)));
            Dynamic positionSource = result.createMap(Map.of(result.createString("type"), result.createString("minecraft:block"), result.createString("pos"), blockPos));
            return result.set("destination", positionSource).set("arrival_in_ticks", result.createInt(arrivalInTicks));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse particle options: {}", (Object)contents, (Object)e);
            return result;
        }
    }

    private <T> Dynamic<T> updateShriek(Dynamic<T> result, String contents) {
        try {
            StringReader reader = new StringReader(contents);
            int delay = reader.readInt();
            return result.set("delay", result.createInt(delay));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse particle options: {}", (Object)contents, (Object)e);
            return result;
        }
    }
}

