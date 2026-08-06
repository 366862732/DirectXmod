/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block.dispatch;

import com.mojang.serialization.Codec;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class SingleVariant
implements BlockStateModel {
    private final BlockStateModelPart model;

    public SingleVariant(BlockStateModelPart model) {
        this.model = model;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(this.model);
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.model.particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.model.materialFlags();
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Variant variant) implements BlockStateModel.Unbaked
    {
        public static final Codec<Unbaked> CODEC = Variant.CODEC.xmap(Unbaked::new, Unbaked::variant);

        @Override
        public BlockStateModel bake(ModelBaker modelBakery) {
            return new SingleVariant(this.variant.bake(modelBakery));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.variant.resolveDependencies(resolver);
        }
    }
}

