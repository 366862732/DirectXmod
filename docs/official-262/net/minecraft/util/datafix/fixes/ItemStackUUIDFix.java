/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.AbstractUUIDFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackUUIDFix
extends AbstractUUIDFix {
    public ItemStackUUIDFix(Schema outputSchema) {
        super(outputSchema, References.ITEM_STACK);
    }

    public TypeRewriteRule makeRule() {
        OpticFinder idF = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        return this.fixTypeEverywhereTyped("ItemStackUUIDFix", this.getInputSchema().getType(this.typeReference), input -> {
            OpticFinder itemTagFinder = input.getType().findField("tag");
            return input.updateTyped(itemTagFinder, typedTag -> typedTag.update(DSL.remainderFinder(), tag -> {
                tag = this.updateAttributeModifiers((Dynamic<?>)tag);
                if (input.getOptional(idF).map(idPair -> "minecraft:player_head".equals(idPair.getSecond())).orElse(false).booleanValue()) {
                    tag = this.updateSkullOwner((Dynamic<?>)tag);
                }
                return tag;
            }));
        });
    }

    private Dynamic<?> updateAttributeModifiers(Dynamic<?> tag) {
        return tag.update("AttributeModifiers", modifiers -> tag.createList(modifiers.asStream().map(modifier -> ItemStackUUIDFix.replaceUUIDLeastMost(modifier, "UUID", "UUID").orElse((Dynamic<?>)modifier))));
    }

    private Dynamic<?> updateSkullOwner(Dynamic<?> tag) {
        return tag.update("SkullOwner", skullOwner -> ItemStackUUIDFix.replaceUUIDString(skullOwner, "Id", "Id").orElse((Dynamic<?>)skullOwner));
    }
}

