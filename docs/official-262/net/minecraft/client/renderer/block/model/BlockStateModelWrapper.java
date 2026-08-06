/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class BlockStateModelWrapper
implements BlockModel {
    private final BlockStateModel model;
    private final List<BlockTintSource> tints;
    private final Matrix4fc transformation;

    public BlockStateModelWrapper(BlockStateModel model, List<BlockTintSource> tints, Matrix4fc transformation) {
        this.model = model;
        this.tints = tints;
        this.transformation = transformation;
    }

    @Override
    public void update(BlockModelRenderState output, BlockState blockState, BlockDisplayContext displayContext, long seed) {
        List<BlockStateModelPart> partList = output.setupModel(this.transformation, this.model.hasMaterialFlag(1));
        this.model.collectParts(output.scratchRandomSource(seed), partList);
        this.updateTints(output, blockState);
    }

    private void updateTints(BlockModelRenderState renderState, BlockState blockState) {
        if (!this.tints.isEmpty()) {
            IntList tintLayers = renderState.tintLayers();
            for (BlockTintSource tint : this.tints) {
                tintLayers.add(tint.color(blockState));
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(BlockState model, List<BlockTintSource> tints, Optional<Transformation> transformation) implements BlockModel.Unbaked
    {
        @Override
        public BlockModel bake(BlockModel.BakingContext context, Matrix4fc transformation) {
            BlockStateModel baseModel = context.modelGetter().apply(this.model);
            Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
            return new BlockStateModelWrapper(baseModel, this.tints, modelTransform);
        }
    }
}

