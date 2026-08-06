/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.base.Splitter
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 *  org.apache.commons.lang3.math.NumberUtils
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.EntityBlockStateFix;
import net.minecraft.util.datafix.fixes.References;
import org.apache.commons.lang3.math.NumberUtils;

public class LevelFlatGeneratorInfoFix
extends DataFix {
    private static final String GENERATOR_OPTIONS = "generatorOptions";
    @VisibleForTesting
    static final String DEFAULT = "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;1;village";
    private static final Splitter SPLITTER = Splitter.on((char)';').limit(5);
    private static final Splitter LAYER_SPLITTER = Splitter.on((char)',');
    private static final Splitter OLD_AMOUNT_SPLITTER = Splitter.on((char)'x').limit(2);
    private static final Splitter AMOUNT_SPLITTER = Splitter.on((char)'*').limit(2);
    private static final Splitter BLOCK_SPLITTER = Splitter.on((char)':').limit(3);

    public LevelFlatGeneratorInfoFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("LevelFlatGeneratorInfoFix", this.getInputSchema().getType(References.LEVEL), input -> input.update(DSL.remainderFinder(), this::fix));
    }

    private Dynamic<?> fix(Dynamic<?> input) {
        if (input.get("generatorName").asString("").equalsIgnoreCase("flat")) {
            return input.update(GENERATOR_OPTIONS, options -> (Dynamic)DataFixUtils.orElse((Optional)options.asString().map(this::fixString).map(arg_0 -> ((Dynamic)options).createString(arg_0)).result(), (Object)options));
        }
        return input;
    }

    @VisibleForTesting
    String fixString(String generatorOptions) {
        String layerInfo;
        int version;
        if (generatorOptions.isEmpty()) {
            return DEFAULT;
        }
        Iterator parts = SPLITTER.split((CharSequence)generatorOptions).iterator();
        String firstPart = (String)parts.next();
        if (parts.hasNext()) {
            version = NumberUtils.toInt((String)firstPart, (int)0);
            layerInfo = (String)parts.next();
        } else {
            version = 0;
            layerInfo = firstPart;
        }
        if (version < 0 || version > 3) {
            return DEFAULT;
        }
        StringBuilder result = new StringBuilder();
        Splitter heightSplitter = version < 3 ? OLD_AMOUNT_SPLITTER : AMOUNT_SPLITTER;
        result.append(StreamSupport.stream(LAYER_SPLITTER.split((CharSequence)layerInfo).spliterator(), false).map(layerString -> {
            String layerType;
            int height;
            List list = heightSplitter.splitToList((CharSequence)layerString);
            if (list.size() == 2) {
                height = NumberUtils.toInt((String)((String)list.get(0)));
                layerType = (String)list.get(1);
            } else {
                height = 1;
                layerType = (String)list.get(0);
            }
            List layerParts = BLOCK_SPLITTER.splitToList((CharSequence)layerType);
            int nameIndex = ((String)layerParts.get(0)).equals("minecraft") ? 1 : 0;
            String blockString = (String)layerParts.get(nameIndex);
            int blockId = version == 3 ? EntityBlockStateFix.getBlockId("minecraft:" + blockString) : NumberUtils.toInt((String)blockString, (int)0);
            int dataIndex = nameIndex + 1;
            int data = layerParts.size() > dataIndex ? NumberUtils.toInt((String)((String)layerParts.get(dataIndex)), (int)0) : 0;
            return (String)(height == 1 ? "" : height + "*") + BlockStateData.getTag(blockId << 4 | data).get("Name").asString("");
        }).collect(Collectors.joining(",")));
        while (parts.hasNext()) {
            result.append(';').append((String)parts.next());
        }
        return result.toString();
    }
}

