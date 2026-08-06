/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class ItemPickupParticleGroup
extends ParticleGroup<ItemPickupParticle> {
    public ItemPickupParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        return new State(this.particles.stream().map(particle -> ParticleInstance.fromParticle(particle, camera, partialTickTime)).toList());
    }

    @Environment(value=EnvType.CLIENT)
    private record State(List<ParticleInstance> instances) implements ParticleGroupRenderState
    {
        @Override
        public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
            PoseStack poseStack = new PoseStack();
            EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            for (ParticleInstance instance : this.instances) {
                entityRenderDispatcher.submit(instance.itemRenderState, camera, instance.xOffset, instance.yOffset, instance.zOffset, poseStack, submitNodeCollector);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record ParticleInstance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {
        public static ParticleInstance fromParticle(ItemPickupParticle particle, Camera camera, float partialTickTime) {
            float time = ((float)particle.life + partialTickTime) / 3.0f;
            time *= time;
            double xt = Mth.lerp((double)partialTickTime, particle.targetXOld, particle.targetX);
            double yt = Mth.lerp((double)partialTickTime, particle.targetYOld, particle.targetY);
            double zt = Mth.lerp((double)partialTickTime, particle.targetZOld, particle.targetZ);
            double xx = Mth.lerp((double)time, particle.itemRenderState.x, xt);
            double yy = Mth.lerp((double)time, particle.itemRenderState.y, yt);
            double zz = Mth.lerp((double)time, particle.itemRenderState.z, zt);
            Vec3 pos = camera.position();
            return new ParticleInstance(particle.itemRenderState, xx - pos.x(), yy - pos.y(), zz - pos.z());
        }
    }
}

