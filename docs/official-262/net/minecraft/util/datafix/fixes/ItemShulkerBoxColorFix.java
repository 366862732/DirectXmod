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
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemShulkerBoxColorFix
extends DataFix {
    public static final String[] NAMES_BY_COLOR = new String[]{"minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:silver_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box", "minecraft:black_shulker_box"};

    public ItemShulkerBoxColorFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder idF = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder tagF = itemStackType.findField("tag");
        OpticFinder blockEntityF = tagF.type().findField("BlockEntityTag");
        return this.fixTypeEverywhereTyped("ItemShulkerBoxColorFix", itemStackType, input -> {
            Typed tag;
            Optional blockEntityOpt;
            Optional tagOpt;
            Optional idOpt = input.getOptional(idF);
            if (idOpt.isPresent() && Objects.equals(((Pair)idOpt.get()).getSecond(), "minecraft:shulker_box") && (tagOpt = input.getOptionalTyped(tagF)).isPresent() && (blockEntityOpt = (tag = (Typed)tagOpt.get()).getOptionalTyped(blockEntityF)).isPresent()) {
                Typed blockEntity = (Typed)blockEntityOpt.get();
                Dynamic blockEntityRest = (Dynamic)blockEntity.get(DSL.remainderFinder());
                int color = blockEntityRest.get("Color").asInt(0);
                blockEntityRest.remove("Color");
                return input.set(tagF, tag.set(blockEntityF, blockEntity.set(DSL.remainderFinder(), (Object)blockEntityRest))).set(idF, (Object)Pair.of((Object)References.ITEM_NAME.typeName(), (Object)NAMES_BY_COLOR[color % 16]));
            }
            return input;
        });
    }
}

