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
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.fixes.AbstractBlockPropertyFix;

public class BlockPropertyRenameAndFix
extends AbstractBlockPropertyFix {
    private final String blockId;
    private final String oldPropertyName;
    private final String newPropertyName;
    private final UnaryOperator<String> valueFixer;

    public BlockPropertyRenameAndFix(Schema outputSchema, String name, String blockId, String oldPropertyName, String newPropertyName, UnaryOperator<String> valueFixer) {
        super(outputSchema, name);
        this.blockId = blockId;
        this.oldPropertyName = oldPropertyName;
        this.newPropertyName = newPropertyName;
        this.valueFixer = valueFixer;
    }

    @Override
    protected boolean shouldFix(String blockId) {
        return blockId.equals(this.blockId);
    }

    @Override
    protected <T> Dynamic<T> fixProperties(String blockId, Dynamic<T> properties) {
        return properties.renameAndFixField(this.oldPropertyName, this.newPropertyName, dynamic -> dynamic.createString((String)this.valueFixer.apply(dynamic.asString(""))));
    }
}

