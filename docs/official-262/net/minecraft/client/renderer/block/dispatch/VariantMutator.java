/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block.dispatch;

import com.mojang.math.Quadrant;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface VariantMutator
extends UnaryOperator<Variant> {
    public static final VariantProperty<Quadrant> X_ROT = Variant::withXRot;
    public static final VariantProperty<Quadrant> Y_ROT = Variant::withYRot;
    public static final VariantProperty<Quadrant> Z_ROT = Variant::withZRot;
    public static final VariantProperty<Identifier> MODEL = Variant::withModel;
    public static final VariantProperty<Boolean> UV_LOCK = Variant::withUvLock;

    default public VariantMutator then(VariantMutator other) {
        return variant -> (Variant)other.apply((Variant)this.apply(variant));
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface VariantProperty<T> {
        public Variant apply(Variant var1, T var2);

        default public VariantMutator withValue(T value) {
            return variant -> this.apply((Variant)variant, value);
        }
    }
}

