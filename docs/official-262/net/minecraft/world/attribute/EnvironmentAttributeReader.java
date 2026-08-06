/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.attribute;

import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface EnvironmentAttributeReader {
    public static final EnvironmentAttributeReader EMPTY = new EnvironmentAttributeReader(){

        @Override
        public <Value> Value getDimensionValue(EnvironmentAttribute<Value> attribute) {
            return attribute.defaultValue();
        }

        @Override
        public <Value> Value getValue(EnvironmentAttribute<Value> attribute, Vec3 pos, @Nullable SpatialAttributeInterpolator biomeInterpolator) {
            return attribute.defaultValue();
        }
    };

    public <Value> Value getDimensionValue(EnvironmentAttribute<Value> var1);

    default public <Value> Value getValue(EnvironmentAttribute<Value> attribute, BlockPos pos) {
        return this.getValue(attribute, Vec3.atCenterOf(pos));
    }

    default public <Value> Value getValue(EnvironmentAttribute<Value> attribute, Vec3 pos) {
        return this.getValue(attribute, pos, null);
    }

    public <Value> Value getValue(EnvironmentAttribute<Value> var1, Vec3 var2, @Nullable SpatialAttributeInterpolator var3);

    default public <Value> Value getValue(LootContext context, EnvironmentAttribute<Value> attribute) {
        if (attribute.isPositional()) {
            return this.getValue(attribute, context.getParameter(LootContextParams.ORIGIN));
        }
        return this.getDimensionValue(attribute);
    }
}

