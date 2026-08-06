/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.material;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidIds;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;

public class Fluids {
    public static final Fluid EMPTY = Fluids.register(FluidIds.EMPTY, new EmptyFluid());
    public static final FlowingFluid FLOWING_WATER = Fluids.register(FluidIds.FLOWING_WATER, new WaterFluid.Flowing());
    public static final FlowingFluid WATER = Fluids.register(FluidIds.WATER, new WaterFluid.Source());
    public static final FlowingFluid FLOWING_LAVA = Fluids.register(FluidIds.FLOWING_LAVA, new LavaFluid.Flowing());
    public static final FlowingFluid LAVA = Fluids.register(FluidIds.LAVA, new LavaFluid.Source());

    private static <T extends Fluid> T register(ResourceKey<Fluid> id, T fluid) {
        return Registry.register(BuiltInRegistries.FLUID, id, fluid);
    }

    static {
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            for (FluidState state : fluid.getStateDefinition().getPossibleStates()) {
                Fluid.FLUID_STATE_REGISTRY.add(state);
            }
        }
    }
}

