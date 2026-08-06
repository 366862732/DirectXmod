/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityWriteReadFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveEmptyItemInBrushableBlockFix
extends NamedEntityWriteReadFix {
    public RemoveEmptyItemInBrushableBlockFix(Schema outputSchema) {
        super(outputSchema, false, "RemoveEmptyItemInSuspiciousBlockFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> input) {
        Optional item = input.get("item").result();
        if (item.isPresent() && RemoveEmptyItemInBrushableBlockFix.isEmptyStack((Dynamic)item.get())) {
            return input.remove("item");
        }
        return input;
    }

    private static boolean isEmptyStack(Dynamic<?> item) {
        String id = NamespacedSchema.ensureNamespaced(item.get("id").asString("minecraft:air"));
        int count = item.get("count").asInt(0);
        return id.equals("minecraft:air") || count == 0;
    }
}

