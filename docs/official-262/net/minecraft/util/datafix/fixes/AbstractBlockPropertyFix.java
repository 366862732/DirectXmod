/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class AbstractBlockPropertyFix
extends DataFix {
    private final String name;

    public AbstractBlockPropertyFix(Schema outputSchema, String name) {
        super(outputSchema, false);
        this.name = name;
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(this.name, this.getInputSchema().getType(References.BLOCK_STATE), input -> input.update(DSL.remainderFinder(), this::fixBlockState));
    }

    private Dynamic<?> fixBlockState(Dynamic<?> tag) {
        Optional<String> blockId = tag.get("Name").asString().result().map(NamespacedSchema::ensureNamespaced);
        if (blockId.isPresent() && this.shouldFix(blockId.get())) {
            return tag.update("Properties", properties -> this.fixProperties((String)blockId.get(), (Dynamic)properties));
        }
        return tag;
    }

    protected abstract boolean shouldFix(String var1);

    protected abstract <T> Dynamic<T> fixProperties(String var1, Dynamic<T> var2);
}

