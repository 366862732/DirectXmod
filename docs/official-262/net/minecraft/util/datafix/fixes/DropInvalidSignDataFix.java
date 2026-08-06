/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Streams
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.BlockEntitySignDoubleSidedEditableTextFix;
import net.minecraft.util.datafix.fixes.References;

public class DropInvalidSignDataFix
extends DataFix {
    private final String entityName;

    public DropInvalidSignDataFix(Schema outputSchema, String entityName) {
        super(outputSchema, false);
        this.entityName = entityName;
    }

    private <T> Dynamic<T> fix(Dynamic<T> tag) {
        tag = tag.update("front_text", DropInvalidSignDataFix::fixText);
        tag = tag.update("back_text", DropInvalidSignDataFix::fixText);
        for (String field : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
            tag = tag.remove(field);
        }
        return tag;
    }

    private static <T> Dynamic<T> fixText(Dynamic<T> tag) {
        Optional filteredLines = tag.get("filtered_messages").asStreamOpt().result();
        if (filteredLines.isEmpty()) {
            return tag;
        }
        Dynamic emptyComponent = LegacyComponentDataFixUtils.createEmptyComponent(tag.getOps());
        List<Dynamic> lines = tag.get("messages").asStreamOpt().result().orElse(Stream.of(new Dynamic[0])).toList();
        List newFilteredLines = Streams.mapWithIndex((Stream)((Stream)filteredLines.get()), (line, index) -> {
            Dynamic fallbackLine = index < (long)lines.size() ? (Dynamic)lines.get((int)index) : emptyComponent;
            return line.equals((Object)emptyComponent) ? fallbackLine : line;
        }).toList();
        if (newFilteredLines.equals(lines)) {
            return tag.remove("filtered_messages");
        }
        return tag.set("filtered_messages", tag.createList(newFilteredLines.stream()));
    }

    public TypeRewriteRule makeRule() {
        Type entityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
        Type entityChoiceType = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, this.entityName);
        OpticFinder entityF = DSL.namedChoice((String)this.entityName, (Type)entityChoiceType);
        return this.fixTypeEverywhereTyped("DropInvalidSignDataFix for " + this.entityName, entityType, input -> input.updateTyped(entityF, entityChoiceType, entity -> {
            boolean filteredCorrect = ((Dynamic)entity.get(DSL.remainderFinder())).get("_filtered_correct").asBoolean(false);
            if (filteredCorrect) {
                return entity.update(DSL.remainderFinder(), remainder -> remainder.remove("_filtered_correct"));
            }
            return Util.writeAndReadTypedOrThrow(entity, entityChoiceType, this::fix);
        }));
    }
}

