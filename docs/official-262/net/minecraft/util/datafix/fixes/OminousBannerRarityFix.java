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
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class OminousBannerRarityFix
extends DataFix {
    public OminousBannerRarityFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public TypeRewriteRule makeRule() {
        Type blockEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        TaggedChoice.TaggedChoiceType blockEntityIdFinder = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
        OpticFinder itemStackIdFinder = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder blockEntityComponentsFieldFinder = blockEntityType.findField("components");
        OpticFinder itemStackComponentsFieldFinder = itemStackType.findField("components");
        OpticFinder itemNameFinder = blockEntityComponentsFieldFinder.type().findField("minecraft:item_name");
        OpticFinder textComponentFinder = DSL.typeFinder((Type)this.getInputSchema().getType(References.TEXT_COMPONENT));
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhereTyped("Ominous Banner block entity common rarity to uncommon rarity fix", blockEntityType, input -> {
            Object blockEntityId = ((Pair)input.get(blockEntityIdFinder.finder())).getFirst();
            return blockEntityId.equals("minecraft:banner") ? this.fix((Typed<?>)input, (OpticFinder<?>)blockEntityComponentsFieldFinder, (OpticFinder<?>)itemNameFinder, (OpticFinder<Pair<String, String>>)textComponentFinder) : input;
        }), (TypeRewriteRule)this.fixTypeEverywhereTyped("Ominous Banner item stack common rarity to uncommon rarity fix", itemStackType, input -> {
            String itemStackId = input.getOptional(itemStackIdFinder).map(Pair::getSecond).orElse("");
            return itemStackId.equals("minecraft:white_banner") ? this.fix((Typed<?>)input, (OpticFinder<?>)itemStackComponentsFieldFinder, (OpticFinder<?>)itemNameFinder, (OpticFinder<Pair<String, String>>)textComponentFinder) : input;
        }));
    }

    private Typed<?> fix(Typed<?> input, OpticFinder<?> componentsFieldFinder, OpticFinder<?> itemNameFinder, OpticFinder<Pair<String, String>> textComponentFinder) {
        return input.updateTyped(componentsFieldFinder, components -> {
            boolean isOminousBanner = components.getOptionalTyped(itemNameFinder).flatMap(itemName -> itemName.getOptional(textComponentFinder)).map(Pair::getSecond).flatMap(LegacyComponentDataFixUtils::extractTranslationString).filter(e -> e.equals("block.minecraft.ominous_banner")).isPresent();
            if (isOminousBanner) {
                return components.updateTyped(itemNameFinder, itemName -> itemName.set(textComponentFinder, (Object)Pair.of((Object)References.TEXT_COMPONENT.typeName(), (Object)LegacyComponentDataFixUtils.createTranslatableComponentJson("block.minecraft.ominous_banner")))).update(DSL.remainderFinder(), remainder -> remainder.set("minecraft:rarity", remainder.createString("uncommon")));
            }
            return components;
        });
    }
}

