/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.List$ListType
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.stream.LongStream;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.fixes.References;

public class BitStorageAlignFix
extends DataFix {
    private static final int BIT_TO_LONG_SHIFT = 6;
    private static final int HEIGHTMAP_BITS = 9;
    private static final int HEIGHTMAP_SIZE = 256;

    public BitStorageAlignFix(Schema schema) {
        super(schema, false);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        Type levelType = chunkType.findFieldType("Level");
        OpticFinder levelFinder = DSL.fieldFinder((String)"Level", (Type)levelType);
        OpticFinder sectionsFinder = levelFinder.type().findField("Sections");
        Type sectionType = ((List.ListType)sectionsFinder.type()).getElement();
        OpticFinder sectionFinder = DSL.typeFinder((Type)sectionType);
        Type blockStateType = DSL.named((String)References.BLOCK_STATE.typeName(), (Type)DSL.remainderType());
        OpticFinder paletteFinder = DSL.fieldFinder((String)"Palette", (Type)DSL.list((Type)blockStateType));
        return this.fixTypeEverywhereTyped("BitStorageAlignFix", chunkType, this.getOutputSchema().getType(References.CHUNK), chunk -> chunk.updateTyped(levelFinder, level -> this.updateHeightmaps(BitStorageAlignFix.updateSections(sectionsFinder, sectionFinder, paletteFinder, level))));
    }

    private Typed<?> updateHeightmaps(Typed<?> level) {
        return level.update(DSL.remainderFinder(), tag -> tag.update("Heightmaps", heightmaps -> heightmaps.updateMapValues(e -> e.mapSecond(heightmap -> BitStorageAlignFix.updateBitStorage(tag, heightmap, 256, 9)))));
    }

    private static Typed<?> updateSections(OpticFinder<?> sectionsFinder, OpticFinder<?> sectionFinder, OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder, Typed<?> level) {
        return level.updateTyped(sectionsFinder, sections -> sections.updateTyped(sectionFinder, section -> {
            int bits = section.getOptional(paletteFinder).map(palette -> Math.max(4, DataFixUtils.ceillog2((int)palette.size()))).orElse(0);
            if (bits == 0 || Mth.isPowerOfTwo(bits)) {
                return section;
            }
            return section.update(DSL.remainderFinder(), tag -> tag.update("BlockStates", states -> BitStorageAlignFix.updateBitStorage(tag, states, 4096, bits)));
        }));
    }

    private static Dynamic<?> updateBitStorage(Dynamic<?> tag, Dynamic<?> storage, int size, int bits) {
        long[] input = storage.asLongStream().toArray();
        long[] output = BitStorageAlignFix.addPadding(size, bits, input);
        return tag.createLongList(LongStream.of(output));
    }

    public static long[] addPadding(int size, int bits, long[] data) {
        int dataLength = data.length;
        if (dataLength == 0) {
            return data;
        }
        long mask = (1L << bits) - 1L;
        int valuesPerLong = 64 / bits;
        int requiredLength = (size + valuesPerLong - 1) / valuesPerLong;
        long[] result = new long[requiredLength];
        int outputDataIndex = 0;
        int outputStart = 0;
        long outputData = 0L;
        int currentIndex = 0;
        long current = data[0];
        long next = dataLength > 1 ? data[1] : 0L;
        for (int index = 0; index < size; ++index) {
            long valueToInsert;
            int position = index * bits;
            int startData = position >> 6;
            int endData = (index + 1) * bits - 1 >> 6;
            int startBit = position ^ startData << 6;
            if (startData != currentIndex) {
                current = next;
                next = startData + 1 < dataLength ? data[startData + 1] : 0L;
                currentIndex = startData;
            }
            if (startData == endData) {
                valueToInsert = current >>> startBit & mask;
            } else {
                int shiftBits = 64 - startBit;
                valueToInsert = (current >>> startBit | next << shiftBits) & mask;
            }
            int outputEnd = outputStart + bits;
            if (outputEnd >= 64) {
                result[outputDataIndex++] = outputData;
                outputData = valueToInsert;
                outputStart = bits;
                continue;
            }
            outputData |= valueToInsert << outputStart;
            outputStart = outputEnd;
        }
        if (outputData != 0L) {
            result[outputDataIndex] = outputData;
        }
        return result;
    }
}

