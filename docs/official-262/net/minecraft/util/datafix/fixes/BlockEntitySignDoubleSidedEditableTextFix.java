/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Streams
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.NamedEntityWriteReadFix;
import net.minecraft.util.datafix.fixes.References;

public class BlockEntitySignDoubleSidedEditableTextFix
extends NamedEntityWriteReadFix {
    public static final List<String> FIELDS_TO_DROP = List.of("Text1", "Text2", "Text3", "Text4", "FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4", "Color", "GlowingText");
    public static final String FILTERED_CORRECT = "_filtered_correct";
    private static final String DEFAULT_COLOR = "black";

    public BlockEntitySignDoubleSidedEditableTextFix(Schema outputSchema, String name, String entityName) {
        super(outputSchema, true, name, References.BLOCK_ENTITY, entityName);
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> input) {
        input = input.set("front_text", BlockEntitySignDoubleSidedEditableTextFix.fixFrontTextTag(input)).set("back_text", BlockEntitySignDoubleSidedEditableTextFix.createDefaultText(input)).set("is_waxed", input.createBoolean(false)).set(FILTERED_CORRECT, input.createBoolean(true));
        for (String field : FIELDS_TO_DROP) {
            input = input.remove(field);
        }
        return input;
    }

    private static <T> Dynamic<T> fixFrontTextTag(Dynamic<T> tag) {
        Dynamic emptyLine = LegacyComponentDataFixUtils.createEmptyComponent(tag.getOps());
        List<Dynamic> lines = BlockEntitySignDoubleSidedEditableTextFix.getLines(tag, "Text").map(line -> line.orElse(emptyLine)).toList();
        Dynamic text = tag.emptyMap().set("messages", tag.createList(lines.stream())).set("color", tag.get("Color").result().orElse(tag.createString(DEFAULT_COLOR))).set("has_glowing_text", tag.get("GlowingText").result().orElse(tag.createBoolean(false)));
        List<Optional<Dynamic<T>>> filteredLines = BlockEntitySignDoubleSidedEditableTextFix.getLines(tag, "FilteredText").toList();
        if (filteredLines.stream().anyMatch(Optional::isPresent)) {
            text = text.set("filtered_messages", tag.createList(Streams.mapWithIndex(filteredLines.stream(), (line, index) -> {
                Dynamic fallbackLine = (Dynamic)lines.get((int)index);
                return line.orElse(fallbackLine);
            })));
        }
        return text;
    }

    private static <T> Stream<Optional<Dynamic<T>>> getLines(Dynamic<T> tag, String linePrefix) {
        return Stream.of(tag.get(linePrefix + "1").result(), tag.get(linePrefix + "2").result(), tag.get(linePrefix + "3").result(), tag.get(linePrefix + "4").result());
    }

    private static <T> Dynamic<T> createDefaultText(Dynamic<T> tag) {
        return tag.emptyMap().set("messages", BlockEntitySignDoubleSidedEditableTextFix.createEmptyLines(tag)).set("color", tag.createString(DEFAULT_COLOR)).set("has_glowing_text", tag.createBoolean(false));
    }

    private static <T> Dynamic<T> createEmptyLines(Dynamic<T> tag) {
        Dynamic emptyComponent = LegacyComponentDataFixUtils.createEmptyComponent(tag.getOps());
        return tag.createList(Stream.of(emptyComponent, emptyComponent, emptyComponent, emptyComponent));
    }
}

