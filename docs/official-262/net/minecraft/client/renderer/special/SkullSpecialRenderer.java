/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SkullBlock;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SkullSpecialRenderer
implements NoDataSpecialModelRenderer {
    private final SkullModelBase model;
    private final float animation;
    private final RenderType renderType;

    public SkullSpecialRenderer(SkullModelBase model, float animation, RenderType renderType) {
        this.model = model;
        this.animation = animation;
        this.renderType = renderType;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        SkullBlockRenderer.submitSkull(this.animation, poseStack, submitNodeCollector, lightCoords, this.model, this.renderType, outlineColor, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        SkullModelBase.State modelState = new SkullModelBase.State();
        modelState.animationPos = this.animation;
        this.model.setupAnim(modelState);
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(SkullBlock.Type kind, Optional<Identifier> textureOverride, float animation) implements NoDataSpecialModelRenderer.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)SkullBlock.Type.CODEC.fieldOf("kind").forGetter(Unbaked::kind), (App)Identifier.CODEC.optionalFieldOf("texture").forGetter(Unbaked::textureOverride), (App)Codec.FLOAT.optionalFieldOf("animation", (Object)Float.valueOf(0.0f)).forGetter(Unbaked::animation)).apply((Applicative)i, Unbaked::new));

        public Unbaked(SkullBlock.Type kind) {
            this(kind, Optional.empty(), 0.0f);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        public @Nullable SkullSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            SkullModelBase model = SkullBlockRenderer.createModel(context.entityModelSet(), this.kind);
            Identifier textureOverride = this.textureOverride.map(t -> t.withPath(p -> "textures/entity/" + p + ".png")).orElse(null);
            if (model == null) {
                return null;
            }
            RenderType renderType = SkullBlockRenderer.getSkullRenderType(this.kind, textureOverride);
            return new SkullSpecialRenderer(model, this.animation, renderType);
        }
    }
}

