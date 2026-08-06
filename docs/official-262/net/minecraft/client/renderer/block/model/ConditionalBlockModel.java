/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.block.model;

import com.mojang.math.Transformation;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.properties.conditional.ConditionalBlockModelProperty;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class ConditionalBlockModel
implements BlockModel {
    private final ConditionalBlockModelProperty property;
    private final BlockModel onTrue;
    private final BlockModel onFalse;

    public ConditionalBlockModel(ConditionalBlockModelProperty property, BlockModel onTrue, BlockModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    @Override
    public void update(BlockModelRenderState output, BlockState blockState, BlockDisplayContext displayContext, long seed) {
        (this.property.get(blockState) ? this.onTrue : this.onFalse).update(output, blockState, displayContext, seed);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Optional<Transformation> transformation, ConditionalBlockModelProperty property, BlockModel.Unbaked onTrue, BlockModel.Unbaked onFalse) implements BlockModel.Unbaked
    {
        @Override
        public BlockModel bake(BlockModel.BakingContext context, Matrix4fc transformation) {
            Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
            return new ConditionalBlockModel(this.property, this.onTrue.bake(context, childTransform), this.onFalse.bake(context, childTransform));
        }
    }
}

