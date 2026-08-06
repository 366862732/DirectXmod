/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.blockentity;

import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class WallAndGroundTransformations<T> {
    private final Map<Direction, T> wallTransforms;
    private final T[] freeTransformations;

    public WallAndGroundTransformations(Function<Direction, T> wallTransformationFactory, IntFunction<T> freeTransformationFactory, int segments) {
        this.wallTransforms = Util.makeEnumMap(Direction.class, wallTransformationFactory);
        this.freeTransformations = new Object[segments];
        for (int segment = 0; segment < segments; ++segment) {
            this.freeTransformations[segment] = freeTransformationFactory.apply(segment);
        }
    }

    public T wallTransformation(Direction facing) {
        return this.wallTransforms.get(facing);
    }

    public T freeTransformations(int segment) {
        return this.freeTransformations[segment];
    }
}

