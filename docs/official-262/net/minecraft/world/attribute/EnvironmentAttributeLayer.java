/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.attribute;

import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public sealed interface EnvironmentAttributeLayer<Value> {

    @FunctionalInterface
    public static interface Positional<Value>
    extends EnvironmentAttributeLayer<Value> {
        public Value applyPositional(Value var1, Vec3 var2, @Nullable SpatialAttributeInterpolator var3);
    }

    @FunctionalInterface
    public static interface TimeBased<Value>
    extends EnvironmentAttributeLayer<Value> {
        public Value applyTimeBased(Value var1, int var2);
    }

    @FunctionalInterface
    public static interface Constant<Value>
    extends EnvironmentAttributeLayer<Value> {
        public Value applyConstant(Value var1);
    }
}

