/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.fish.TropicalFish;

@Environment(value=EnvType.CLIENT)
public class TropicalFishPatternLayer
extends RenderLayer<TropicalFishRenderState, EntityModel<TropicalFishRenderState>> {
    private static final Identifier KOB_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_1.png");
    private static final Identifier SUNSTREAK_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_2.png");
    private static final Identifier SNOOPER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_3.png");
    private static final Identifier DASHER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_4.png");
    private static final Identifier BRINELY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_5.png");
    private static final Identifier SPOTTY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_6.png");
    private static final Identifier FLOPPER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_1.png");
    private static final Identifier STRIPEY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_2.png");
    private static final Identifier GLITTER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_3.png");
    private static final Identifier BLOCKFISH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_4.png");
    private static final Identifier BETTY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_5.png");
    private static final Identifier CLAYFISH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_6.png");
    private final TropicalFishSmallModel modelSmall;
    private final TropicalFishLargeModel modelLarge;

    public TropicalFishPatternLayer(RenderLayerParent<TropicalFishRenderState, EntityModel<TropicalFishRenderState>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.modelSmall = new TropicalFishSmallModel(modelSet.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL_PATTERN));
        this.modelLarge = new TropicalFishLargeModel(modelSet.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE_PATTERN));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, TropicalFishRenderState state, float yRot, float xRot) {
        TropicalFish.Pattern variant = state.pattern;
        EntityModel model = switch (variant.base()) {
            default -> throw new MatchException(null, null);
            case TropicalFish.Base.SMALL -> this.modelSmall;
            case TropicalFish.Base.LARGE -> this.modelLarge;
        };
        Identifier patternTexture = switch (variant) {
            default -> throw new MatchException(null, null);
            case TropicalFish.Pattern.KOB -> KOB_TEXTURE;
            case TropicalFish.Pattern.SUNSTREAK -> SUNSTREAK_TEXTURE;
            case TropicalFish.Pattern.SNOOPER -> SNOOPER_TEXTURE;
            case TropicalFish.Pattern.DASHER -> DASHER_TEXTURE;
            case TropicalFish.Pattern.BRINELY -> BRINELY_TEXTURE;
            case TropicalFish.Pattern.SPOTTY -> SPOTTY_TEXTURE;
            case TropicalFish.Pattern.FLOPPER -> FLOPPER_TEXTURE;
            case TropicalFish.Pattern.STRIPEY -> STRIPEY_TEXTURE;
            case TropicalFish.Pattern.GLITTER -> GLITTER_TEXTURE;
            case TropicalFish.Pattern.BLOCKFISH -> BLOCKFISH_TEXTURE;
            case TropicalFish.Pattern.BETTY -> BETTY_TEXTURE;
            case TropicalFish.Pattern.CLAYFISH -> CLAYFISH_TEXTURE;
        };
        TropicalFishPatternLayer.coloredCutoutModelCopyLayerRender(model, patternTexture, poseStack, submitNodeCollector, lightCoords, state, state.patternColor, 1);
    }
}

