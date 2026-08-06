/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

@Environment(value=EnvType.CLIENT)
public class FluidStateModelSet {
    private static final FluidModel.Unbaked WATER_MODEL = new FluidModel.Unbaked(new Material(Identifier.withDefaultNamespace("block/water_still")), new Material(Identifier.withDefaultNamespace("block/water_flow")), new Material(Identifier.withDefaultNamespace("block/water_overlay")), BlockTintSources.water());
    private static final FluidModel.Unbaked LAVA_MODEL = new FluidModel.Unbaked(new Material(Identifier.withDefaultNamespace("block/lava_still")), new Material(Identifier.withDefaultNamespace("block/lava_flow")), null, null);
    private final Map<Fluid, FluidModel> modelByFluid;
    private final FluidModel missingModel;

    public FluidStateModelSet(Map<Fluid, FluidModel> modelByFluid, FluidModel missingModel) {
        this.modelByFluid = modelByFluid;
        this.missingModel = missingModel;
    }

    public static Map<Fluid, FluidModel> bake(MaterialBaker materials) {
        FluidModel waterModel = WATER_MODEL.bake(materials, () -> "Water");
        FluidModel lavaModel = LAVA_MODEL.bake(materials, () -> "Lava");
        return Map.of(Fluids.WATER, waterModel, Fluids.FLOWING_WATER, waterModel, Fluids.LAVA, lavaModel, Fluids.FLOWING_LAVA, lavaModel);
    }

    public FluidModel get(FluidState state) {
        return this.modelByFluid.getOrDefault(state.getType(), this.missingModel);
    }
}

