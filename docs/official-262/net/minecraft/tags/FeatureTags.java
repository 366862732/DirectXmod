/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class FeatureTags {
    public static final TagKey<ConfiguredFeature<?, ?>> CAN_SPAWN_FROM_BONE_MEAL = FeatureTags.create("can_spawn_from_bone_meal");

    private FeatureTags() {
    }

    private static TagKey<ConfiguredFeature<?, ?>> create(String name) {
        return TagKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace(name));
    }
}

