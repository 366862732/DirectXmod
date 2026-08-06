/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.DataComponentRemainderFix;
import org.jspecify.annotations.Nullable;

public class TridentAnimationFix
extends DataComponentRemainderFix {
    public TridentAnimationFix(Schema outputSchema) {
        super(outputSchema, "TridentAnimationFix", "minecraft:consumable");
    }

    @Override
    protected <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> input) {
        return input.update("animation", animation -> {
            String optional = animation.asString().result().orElse("");
            if ("spear".equals(optional)) {
                return animation.createString("trident");
            }
            return animation;
        });
    }
}

