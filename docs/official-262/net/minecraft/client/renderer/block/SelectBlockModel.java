/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.block;

import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.properties.select.SelectBlockModelProperty;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SelectBlockModel<T>
implements BlockModel {
    private final SelectBlockModelProperty<T> property;
    private final ModelSelector<T> models;

    public SelectBlockModel(SelectBlockModelProperty<T> property, ModelSelector<T> models) {
        this.property = property;
        this.models = models;
    }

    @Override
    public void update(BlockModelRenderState output, BlockState blockState, BlockDisplayContext displayContext, long seed) {
        T value = this.property.get(blockState, displayContext);
        BlockModel model = this.models.get(value);
        if (model != null) {
            model.update(output, blockState, displayContext, seed);
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface ModelSelector<T> {
        public @Nullable BlockModel get(@Nullable T var1);
    }

    @Environment(value=EnvType.CLIENT)
    public record SwitchCase<T>(List<T> values, BlockModel.Unbaked model) {
    }

    @Environment(value=EnvType.CLIENT)
    public record UnbakedSwitch<P extends SelectBlockModelProperty<T>, T>(P property, List<SwitchCase<T>> cases) {
        public BlockModel bake(BlockModel.BakingContext context, Matrix4fc transformation, BlockModel fallback) {
            Object2ObjectOpenHashMap bakedModels = new Object2ObjectOpenHashMap();
            for (SwitchCase<T> c : this.cases) {
                BlockModel.Unbaked caseModel = c.model;
                BlockModel bakedCaseModel = caseModel.bake(context, transformation);
                for (Object value : c.values) {
                    bakedModels.put(value, (Object)bakedCaseModel);
                }
            }
            bakedModels.defaultReturnValue((Object)fallback);
            return new SelectBlockModel<Object>((SelectBlockModelProperty<Object>)this.property, arg_0 -> ((Object2ObjectMap)bakedModels).get(arg_0));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Optional<Transformation> transformation, UnbakedSwitch<?, ?> unbakedSwitch, Optional<BlockModel.Unbaked> fallback) implements BlockModel.Unbaked
    {
        @Override
        public BlockModel bake(BlockModel.BakingContext context, Matrix4fc transformation) {
            Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
            BlockModel bakedFallback = this.fallback.map(m -> m.bake(context, childTransform)).orElse(context.missingBlockModel());
            return this.unbakedSwitch.bake(context, childTransform, bakedFallback);
        }
    }
}

