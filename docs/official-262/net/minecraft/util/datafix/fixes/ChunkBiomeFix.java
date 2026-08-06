/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.util.datafix.fixes.References;

public class ChunkBiomeFix
extends DataFix {
    public ChunkBiomeFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder levelFinder = chunkType.findField("Level");
        return this.fixTypeEverywhereTyped("Leaves fix", chunkType, chunk -> chunk.updateTyped(levelFinder, level -> level.update(DSL.remainderFinder(), tag -> {
            Optional biomes = tag.get("Biomes").asIntStreamOpt().result();
            if (biomes.isEmpty()) {
                return tag;
            }
            int[] oldBiomes = ((IntStream)biomes.get()).toArray();
            if (oldBiomes.length != 256) {
                return tag;
            }
            int[] newBiomes = new int[1024];
            for (int z = 0; z < 4; ++z) {
                for (int x = 0; x < 4; ++x) {
                    int oldX = (x << 2) + 2;
                    int oldZ = (z << 2) + 2;
                    int index = oldZ << 4 | oldX;
                    newBiomes[z << 2 | x] = oldBiomes[index];
                }
            }
            for (int ySlice = 1; ySlice < 64; ++ySlice) {
                System.arraycopy(newBiomes, 0, newBiomes, ySlice * 16, 16);
            }
            return tag.set("Biomes", tag.createIntList(Arrays.stream(newBiomes)));
        })));
    }
}

