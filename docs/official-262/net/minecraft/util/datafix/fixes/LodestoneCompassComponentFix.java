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
import net.minecraft.util.datafix.fixes.DataComponentRemainderFix;

public class LodestoneCompassComponentFix
extends DataComponentRemainderFix {
    public LodestoneCompassComponentFix(Schema outputSchema) {
        super(outputSchema, "LodestoneCompassComponentFix", "minecraft:lodestone_target", "minecraft:lodestone_tracker");
    }

    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> input) {
        Optional pos = input.get("pos").result();
        Optional dimension = input.get("dimension").result();
        input = input.remove("pos").remove("dimension");
        if (pos.isPresent() && dimension.isPresent()) {
            input = input.set("target", input.emptyMap().set("pos", (Dynamic)pos.get()).set("dimension", (Dynamic)dimension.get()));
        }
        return input;
    }
}

