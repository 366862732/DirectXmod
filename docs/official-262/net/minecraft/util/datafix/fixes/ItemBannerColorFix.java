/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
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

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemBannerColorFix
extends DataFix {
    public ItemBannerColorFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder idF = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder tagF = itemStackType.findField("tag");
        OpticFinder blockEntityF = tagF.type().findField("BlockEntityTag");
        return this.fixTypeEverywhereTyped("ItemBannerColorFix", itemStackType, input -> {
            Optional id = input.getOptional(idF);
            if (id.isPresent() && Objects.equals(((Pair)id.get()).getSecond(), "minecraft:banner")) {
                Typed tag;
                Optional blockEntityOpt;
                Dynamic rest = (Dynamic)input.get(DSL.remainderFinder());
                Optional tagOpt = input.getOptionalTyped(tagF);
                if (tagOpt.isPresent() && (blockEntityOpt = (tag = (Typed)tagOpt.get()).getOptionalTyped(blockEntityF)).isPresent()) {
                    Typed blockEntity = (Typed)blockEntityOpt.get();
                    Dynamic tagRest = (Dynamic)tag.get(DSL.remainderFinder());
                    Dynamic blockEntityRest = (Dynamic)blockEntity.getOrCreate(DSL.remainderFinder());
                    if (blockEntityRest.get("Base").asNumber().result().isPresent()) {
                        Dynamic pickMarker;
                        Dynamic display;
                        rest = rest.set("Damage", rest.createShort((short)(blockEntityRest.get("Base").asInt(0) & 0xF)));
                        Optional displayOptional = tagRest.get("display").result();
                        if (displayOptional.isPresent() && Objects.equals(display = (Dynamic)displayOptional.get(), pickMarker = display.createMap((Map)ImmutableMap.of((Object)display.createString("Lore"), (Object)display.createList(Stream.of(display.createString("(+NBT"))))))) {
                            return input.set(DSL.remainderFinder(), (Object)rest);
                        }
                        blockEntityRest.remove("Base");
                        return input.set(DSL.remainderFinder(), (Object)rest).set(tagF, tag.set(blockEntityF, blockEntity.set(DSL.remainderFinder(), (Object)blockEntityRest)));
                    }
                }
                return input.set(DSL.remainderFinder(), (Object)rest);
            }
            return input;
        });
    }
}

