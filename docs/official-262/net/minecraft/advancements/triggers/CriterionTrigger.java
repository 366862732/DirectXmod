/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.triggers.Criterion;

public interface CriterionTrigger<T extends CriterionTriggerInstance> {
    public Codec<T> codec();

    default public Criterion<T> createCriterion(T instance) {
        return new Criterion<T>(this, instance);
    }
}

