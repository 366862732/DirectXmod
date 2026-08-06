/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PlayerHeadSpecialRenderer
implements SpecialModelRenderer<PlayerSkinRenderCache.RenderInfo> {
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final SkullModelBase modelBase;

    private PlayerHeadSpecialRenderer(PlayerSkinRenderCache playerSkinRenderCache, SkullModelBase modelBase) {
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.modelBase = modelBase;
    }

    @Override
    public void submit(@Nullable PlayerSkinRenderCache.RenderInfo argument, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        RenderType renderType = argument != null ? argument.renderType() : PlayerSkinRenderCache.DEFAULT_PLAYER_SKIN_RENDER_TYPE;
        SkullBlockRenderer.submitSkull(0.0f, poseStack, submitNodeCollector, lightCoords, this.modelBase, renderType, outlineColor, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.modelBase.root().getExtentsForGui(poseStack, output);
    }

    @Override
    public @Nullable PlayerSkinRenderCache.RenderInfo extractArgument(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) {
            return null;
        }
        return this.playerSkinRenderCache.getOrDefault(profile);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked<PlayerSkinRenderCache.RenderInfo>
    {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        public PlayerHeadSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            SkullModel model = new SkullModel(context.entityModelSet().bakeLayer(ModelLayers.PLAYER_HEAD));
            return new PlayerHeadSpecialRenderer(context.playerSkinRenderCache(), model);
        }
    }
}

