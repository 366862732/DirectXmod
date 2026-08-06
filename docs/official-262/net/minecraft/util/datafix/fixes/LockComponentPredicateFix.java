/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.escape.Escaper
 *  com.google.common.escape.Escapers
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.DataComponentRemainderFix;
import org.jspecify.annotations.Nullable;

public class LockComponentPredicateFix
extends DataComponentRemainderFix {
    public static final Escaper ESCAPER = Escapers.builder().addEscape('\"', "\\\"").addEscape('\\', "\\\\").build();

    public LockComponentPredicateFix(Schema outputSchema) {
        super(outputSchema, "LockComponentPredicateFix", "minecraft:lock");
    }

    @Override
    protected <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> input) {
        return LockComponentPredicateFix.fixLock(input);
    }

    public static <T> @Nullable Dynamic<T> fixLock(Dynamic<T> input) {
        Optional name = input.asString().result();
        if (name.isEmpty()) {
            return null;
        }
        if (((String)name.get()).isEmpty()) {
            return null;
        }
        Dynamic nameComponent = input.createString("\"" + ESCAPER.escape((String)name.get()) + "\"");
        Dynamic components = input.emptyMap().set("minecraft:custom_name", nameComponent);
        return input.emptyMap().set("components", components);
    }
}

