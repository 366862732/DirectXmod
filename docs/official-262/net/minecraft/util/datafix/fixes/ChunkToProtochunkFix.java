/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.shorts.ShortArrayList
 *  it.unimi.dsi.fastutil.shorts.ShortList
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.util.datafix.fixes.References;

public class ChunkToProtochunkFix
extends DataFix {
    private static final int NUM_SECTIONS = 16;

    public ChunkToProtochunkFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.writeFixAndRead("ChunkToProtoChunkFix", this.getInputSchema().getType(References.CHUNK), this.getOutputSchema().getType(References.CHUNK), chunk -> chunk.update("Level", ChunkToProtochunkFix::fixChunkData));
    }

    private static <T> Dynamic<T> fixChunkData(Dynamic<T> tag) {
        boolean lightPopulated;
        boolean terrainPopulated = tag.get("TerrainPopulated").asBoolean(false);
        boolean bl = lightPopulated = tag.get("LightPopulated").asNumber().result().isEmpty() || tag.get("LightPopulated").asBoolean(false);
        String status = terrainPopulated ? (lightPopulated ? "mobs_spawned" : "decorated") : "carved";
        return ChunkToProtochunkFix.repackTicks(ChunkToProtochunkFix.repackBiomes(tag)).set("Status", tag.createString(status)).set("hasLegacyStructureData", tag.createBoolean(true));
    }

    private static <T> Dynamic<T> repackBiomes(Dynamic<T> tag) {
        return tag.update("Biomes", biomes -> (Dynamic)DataFixUtils.orElse(biomes.asByteBufferOpt().result().map(buffer -> {
            int[] newBiomes = new int[256];
            for (int i = 0; i < newBiomes.length; ++i) {
                if (i >= buffer.capacity()) continue;
                newBiomes[i] = buffer.get(i) & 0xFF;
            }
            return tag.createIntList(Arrays.stream(newBiomes));
        }), (Object)biomes));
    }

    private static <T> Dynamic<T> repackTicks(Dynamic<T> tag) {
        return (Dynamic)DataFixUtils.orElse(tag.get("TileTicks").asStreamOpt().result().map(ticks -> {
            List toBeTickedTag = IntStream.range(0, 16).mapToObj(i -> new ShortArrayList()).collect(Collectors.toList());
            ticks.forEach(pendingTickTag -> {
                int x = pendingTickTag.get("x").asInt(0);
                int y = pendingTickTag.get("y").asInt(0);
                int z = pendingTickTag.get("z").asInt(0);
                short packedOffset = ChunkToProtochunkFix.packOffsetCoordinates(x, y, z);
                ((ShortList)toBeTickedTag.get(y >> 4)).add(packedOffset);
            });
            return tag.remove("TileTicks").set("ToBeTicked", tag.createList(toBeTickedTag.stream().map(l -> tag.createList(l.intStream().mapToObj(v -> tag.createShort((short)v))))));
        }), tag);
    }

    private static short packOffsetCoordinates(int x, int y, int z) {
        return (short)(x & 0xF | (y & 0xF) << 4 | (z & 0xF) << 8);
    }
}

