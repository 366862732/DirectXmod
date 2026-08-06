/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllusionerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class IllusionerRenderer
extends IllagerRenderer<Illusioner, IllusionerRenderState> {
    private static final Identifier ILLUSIONER = Identifier.withDefaultNamespace("textures/entity/illager/illusioner.png");

    public IllusionerRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel(context.bakeLayer(ModelLayers.ILLUSIONER)), 0.5f);
        this.addLayer(new ItemInHandLayer<IllusionerRenderState, IllagerModel<IllusionerRenderState>>(this, (RenderLayerParent)this){
            {
                Objects.requireNonNull(this$0);
                super(renderer);
            }

            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, IllusionerRenderState state, float yRot, float xRot) {
                if (state.isCastingSpell || state.isAggressive) {
                    super.submit(poseStack, submitNodeCollector, lightCoords, state, yRot, xRot);
                }
            }
        });
        ((IllagerModel)this.model).getHat().visible = true;
    }

    @Override
    public Identifier getTextureLocation(IllusionerRenderState state) {
        return ILLUSIONER;
    }

    @Override
    public IllusionerRenderState createRenderState() {
        return new IllusionerRenderState();
    }

    @Override
    public void extractRenderState(Illusioner entity, IllusionerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Vec3[] illusionOffsets = entity.getIllusionOffsets(partialTicks);
        state.illusionOffsets = Arrays.copyOf(illusionOffsets, illusionOffsets.length);
        state.isCastingSpell = entity.isCastingSpell();
    }

    @Override
    public void submit(IllusionerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.isInvisible) {
            Vec3[] offsets = state.illusionOffsets;
            for (int i = 0; i < offsets.length; ++i) {
                poseStack.pushPose();
                poseStack.translate(offsets[i].x + (double)Mth.cos((float)i + state.ageInTicks * 0.5f) * 0.025, offsets[i].y + (double)Mth.cos((float)i + state.ageInTicks * 0.75f) * 0.0125, offsets[i].z + (double)Mth.cos((float)i + state.ageInTicks * 0.7f) * 0.025);
                super.submit(state, poseStack, submitNodeCollector, camera);
                poseStack.popPose();
            }
        } else {
            super.submit(state, poseStack, submitNodeCollector, camera);
        }
    }

    @Override
    protected boolean isBodyVisible(IllusionerRenderState state) {
        return true;
    }

    @Override
    protected AABB getBoundingBoxForCulling(Illusioner entity) {
        return super.getBoundingBoxForCulling(entity).inflate(3.0, 0.0, 3.0);
    }
}

