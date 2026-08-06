/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ItemFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Item");
    public static final Identifier ENCHANTED_GLINT_ARMOR = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png");
    public static final Identifier ENCHANTED_GLINT_ITEM = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final float SPECIAL_FOIL_UI_SCALE = 0.5f;
    private static final float SPECIAL_FOIL_FIRST_PERSON_SCALE = 0.75f;
    private static final float SPECIAL_FOIL_TEXTURE_SCALE = 0.0078125f;
    public static final int NO_TINT = -1;
    private final QuadInstance quadInstance = new QuadInstance();

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        for (Submit submit : submits) {
            this.prepareSubmit(submit, false);
        }
        for (Submit submit : submits) {
            this.prepareSubmit(submit, true);
        }
    }

    private void prepareSubmit(Submit submit, boolean foil) {
        if (foil) {
            this.prepareFoilSubmit(submit);
        } else if (submit.outlineColor() != 0) {
            this.prepareOutlineSubmit(submit);
        } else {
            this.prepareMainSubmit(submit);
        }
    }

    private void prepareMainSubmit(Submit submit) {
        this.quadInstance.setLightCoords(submit.lightCoords());
        this.quadInstance.setOverlayCoords(submit.overlayCoords());
        for (BakedQuad quad : submit.quads()) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            RenderType renderType = material.itemRenderType();
            this.quadInstance.setColor(ItemFeatureRenderer.getLayerColorSafe(submit.tintLayers(), material));
            this.getVertexBuilder(renderType).putBakedQuad(submit.pose(), quad, this.quadInstance);
        }
    }

    private void prepareOutlineSubmit(Submit submit) {
        for (BakedQuad quad : submit.quads()) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            RenderType renderType = material.itemRenderType().outline().orElse(null);
            if (renderType == null) continue;
            this.quadInstance.setColor(submit.outlineColor());
            this.getVertexBuilder(renderType).putBakedQuad(submit.pose(), quad, this.quadInstance);
        }
    }

    private void prepareFoilSubmit(Submit submit) {
        ItemStackRenderState.FoilType foilType = submit.foilType();
        if (foilType == ItemStackRenderState.FoilType.NONE) {
            return;
        }
        PoseStack.Pose foilDecalPose = foilType == ItemStackRenderState.FoilType.SPECIAL ? ItemFeatureRenderer.computeFoilDecalPose(submit.displayContext(), submit.pose()) : null;
        for (BakedQuad quad : submit.quads()) {
            VertexConsumer foilBuffer = this.getFoilBuffer(quad.materialInfo().itemRenderType(), foilDecalPose);
            foilBuffer.putBakedQuad(submit.pose(), quad, this.quadInstance);
        }
    }

    private VertexConsumer getFoilBuffer(RenderType renderType, @Nullable PoseStack.Pose foilDecalPose) {
        RenderType foilRenderType = ItemFeatureRenderer.useTransparentGlint(renderType) ? RenderTypes.glintTranslucent() : RenderTypes.glint();
        VertexConsumer foilBuffer = this.getVertexBuilder(foilRenderType);
        if (foilDecalPose != null) {
            foilBuffer = new SheetedDecalTextureGenerator(foilBuffer, foilDecalPose, 0.0078125f);
        }
        return foilBuffer;
    }

    private static PoseStack.Pose computeFoilDecalPose(ItemDisplayContext type, PoseStack.Pose pose) {
        PoseStack.Pose foilDecalPose = pose.copy();
        if (type == ItemDisplayContext.GUI) {
            MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.5f);
        } else if (type.firstPerson()) {
            MatrixUtil.mulComponentWise(foilDecalPose.pose(), 0.75f);
        }
        return foilDecalPose;
    }

    private static boolean useTransparentGlint(RenderType renderType) {
        return Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency() && renderType.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET;
    }

    private static int getLayerColorSafe(int[] layers, int layer) {
        if (layer < 0 || layer >= layers.length) {
            return -1;
        }
        return layers[layer];
    }

    private static int getLayerColorSafe(int[] tintLayers, BakedQuad.MaterialInfo material) {
        if (material.isTinted()) {
            return ItemFeatureRenderer.getLayerColorSafe(tintLayers, material.tintIndex());
        }
        return -1;
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(PoseStack.Pose pose, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) implements TranslucentSubmit
    {
        public boolean hasTranslucency() {
            for (BakedQuad quad : this.quads()) {
                if (!quad.materialInfo().itemRenderType().hasBlending()) continue;
                return true;
            }
            return false;
        }

        @Override
        public float distanceToCameraSq() {
            return TranslucentSubmit.computeDistanceToCameraSq((Matrix4fc)this.pose.pose());
        }

        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

