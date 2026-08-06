/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.shulker.ShulkerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ShulkerRenderer
extends MobRenderer<Shulker, ShulkerRenderState, ShulkerModel> {
    private static final Identifier DEFAULT_TEXTURE_LOCATION = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION.texture().withPath(path -> "textures/" + path + ".png");
    private static final Identifier[] TEXTURE_LOCATION = (Identifier[])Sheets.SHULKER_TEXTURE_LOCATION.stream().map(location -> location.texture().withPath(path -> "textures/" + path + ".png")).toArray(Identifier[]::new);

    public ShulkerRenderer(EntityRendererProvider.Context context) {
        super(context, new ShulkerModel(context.bakeLayer(ModelLayers.SHULKER)), 0.0f);
    }

    @Override
    public Vec3 getRenderOffset(ShulkerRenderState state) {
        return state.renderOffset;
    }

    @Override
    public boolean shouldRender(Shulker entity, Frustum culler, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, culler, camX, camY, camZ)) {
            return true;
        }
        Vec3 startPos = entity.getRenderPosition(0.0f);
        if (startPos == null) {
            return false;
        }
        EntityType<?> type = entity.getType();
        float halfHeight = type.getHeight() / 2.0f;
        float halfWidth = type.getWidth() / 2.0f;
        Vec3 targetPos = Vec3.atBottomCenterOf(entity.blockPosition());
        return culler.isVisible(new AABB(startPos.x, startPos.y + (double)halfHeight, startPos.z, targetPos.x, targetPos.y + (double)halfHeight, targetPos.z).inflate(halfWidth, halfHeight, halfWidth));
    }

    @Override
    public Identifier getTextureLocation(ShulkerRenderState state) {
        return ShulkerRenderer.getTextureLocation(state.color);
    }

    @Override
    public ShulkerRenderState createRenderState() {
        return new ShulkerRenderState();
    }

    @Override
    public void extractRenderState(Shulker entity, ShulkerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.renderOffset = Objects.requireNonNullElse(entity.getRenderPosition(partialTicks), Vec3.ZERO);
        state.color = entity.getColor();
        state.peekAmount = entity.getClientPeekAmount(partialTicks);
        state.yHeadRot = entity.yHeadRot;
        state.yBodyRot = entity.yBodyRot;
        state.attachFace = entity.getAttachFace();
    }

    public static Identifier getTextureLocation(@Nullable DyeColor color) {
        if (color == null) {
            return DEFAULT_TEXTURE_LOCATION;
        }
        return TEXTURE_LOCATION[color.getId()];
    }

    @Override
    protected void setupRotations(ShulkerRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot + 180.0f, entityScale);
        poseStack.rotateAround((Quaternionfc)state.attachFace.getOpposite().getRotation(), 0.0f, 0.5f, 0.0f);
    }
}

