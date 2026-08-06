/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.TaggedChoice$TaggedChoiceType
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public class BannerEntityCustomNameToOverrideComponentFix
extends DataFix {
    public BannerEntityCustomNameToOverrideComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public TypeRewriteRule makeRule() {
        Type blockEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
        TaggedChoice.TaggedChoiceType blockEntityIdFinder = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
        OpticFinder customNameFinder = blockEntityType.findField("CustomName");
        OpticFinder textComponentFinder = DSL.typeFinder((Type)this.getInputSchema().getType(References.TEXT_COMPONENT));
        return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", blockEntityType, input -> {
            Object blockEntityId = ((Pair)input.get(blockEntityIdFinder.finder())).getFirst();
            return blockEntityId.equals("minecraft:banner") ? this.fix((Typed<?>)input, (OpticFinder<Pair<String, String>>)textComponentFinder, (OpticFinder<?>)customNameFinder) : input;
        });
    }

    private Typed<?> fix(Typed<?> input, OpticFinder<Pair<String, String>> textComponentFinder, OpticFinder<?> customNameFinder) {
        Optional customName = input.getOptionalTyped(customNameFinder).flatMap(name -> name.getOptional(textComponentFinder).map(Pair::getSecond));
        boolean isOminousBanner = customName.flatMap(LegacyComponentDataFixUtils::extractTranslationString).filter(e -> e.equals("block.minecraft.ominous_banner")).isPresent();
        if (isOminousBanner) {
            return Util.writeAndReadTypedOrThrow(input, input.getType(), dynamic -> {
                Dynamic components = dynamic.createMap(Map.of(dynamic.createString("minecraft:item_name"), dynamic.createString((String)customName.get()), dynamic.createString("minecraft:hide_additional_tooltip"), dynamic.emptyMap()));
                return dynamic.set("components", components).remove("CustomName");
            });
        }
        return input;
    }
}

