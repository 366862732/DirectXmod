/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class CopperGolemStatueSpecialRenderer
implements NoDataSpecialModelRenderer {
    private final CopperGolemStatueModel model;
    private final Identifier texture;

    public CopperGolemStatueSpecialRenderer(CopperGolemStatueModel model, Identifier texture) {
        this.model = model;
        this.texture = texture;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        submitNodeCollector.submitModel(this.model, Unit.INSTANCE, poseStack, this.texture, lightCoords, overlayCoords, outlineColor, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(Unit.INSTANCE);
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Identifier texture, CopperGolemStatueBlock.Pose pose) implements NoDataSpecialModelRenderer.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture), (App)CopperGolemStatueBlock.Pose.CODEC.fieldOf("pose").forGetter(Unbaked::pose)).apply((Applicative)i, Unbaked::new));

        public Unbaked(WeatheringCopper.WeatherState state, CopperGolemStatueBlock.Pose pose) {
            this(CopperGolemOxidationLevels.getOxidationLevel(state).texture(), pose);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        public CopperGolemStatueSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            CopperGolemStatueModel model = new CopperGolemStatueModel(context.entityModelSet().bakeLayer(Unbaked.getModel(this.pose)));
            return new CopperGolemStatueSpecialRenderer(model, this.texture);
        }

        private static ModelLayerLocation getModel(CopperGolemStatueBlock.Pose pose) {
            return switch (pose) {
                default -> throw new MatchException(null, null);
                case CopperGolemStatueBlock.Pose.STANDING -> ModelLayers.COPPER_GOLEM;
                case CopperGolemStatueBlock.Pose.SITTING -> ModelLayers.COPPER_GOLEM_SITTING;
                case CopperGolemStatueBlock.Pose.STAR -> ModelLayers.COPPER_GOLEM_STAR;
                case CopperGolemStatueBlock.Pose.RUNNING -> ModelLayers.COPPER_GOLEM_RUNNING;
            };
        }
    }
}

