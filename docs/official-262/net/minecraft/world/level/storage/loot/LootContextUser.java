/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage.loot;

import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public interface LootContextUser
extends Validatable {
    default public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of();
    }

    @Override
    default public void validate(ValidationContext context) {
        context.validateContextUsage(this);
    }
}

