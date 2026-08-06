/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.References;

public abstract class AbstractPoiSectionFix
extends DataFix {
    private final String name;

    public AbstractPoiSectionFix(Schema outputSchema, String name) {
        super(outputSchema, false);
        this.name = name;
    }

    protected TypeRewriteRule makeRule() {
        Type poiChunkType = DSL.named((String)References.POI_CHUNK.typeName(), (Type)DSL.remainderType());
        if (!Objects.equals(poiChunkType, this.getInputSchema().getType(References.POI_CHUNK))) {
            throw new IllegalStateException("Poi type is not what was expected.");
        }
        return this.fixTypeEverywhere(this.name, poiChunkType, ops -> input -> input.mapSecond(this::cap));
    }

    private <T> Dynamic<T> cap(Dynamic<T> input) {
        return input.update("Sections", sections -> sections.updateMapValues(entry -> entry.mapSecond(this::processSection)));
    }

    private Dynamic<?> processSection(Dynamic<?> section) {
        return section.update("Records", this::processSectionRecords);
    }

    private <T> Dynamic<T> processSectionRecords(Dynamic<T> input) {
        return (Dynamic)DataFixUtils.orElse(input.asStreamOpt().result().map(stream -> input.createList(this.processRecords((Stream)stream))), input);
    }

    protected abstract <T> Stream<Dynamic<T>> processRecords(Stream<Dynamic<T>> var1);
}

