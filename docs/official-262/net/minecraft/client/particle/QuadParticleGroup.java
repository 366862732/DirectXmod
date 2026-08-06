/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;

@Environment(value=EnvType.CLIENT)
public class QuadParticleGroup
extends ParticleGroup<SingleQuadParticle> {
    private final ParticleRenderType particleType;
    private final QuadParticleRenderState particleTypeRenderState = new QuadParticleRenderState();

    public QuadParticleGroup(ParticleEngine engine, ParticleRenderType particleType) {
        super(engine);
        this.particleType = particleType;
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        for (SingleQuadParticle particle : this.particles) {
            if (!frustum.pointInFrustum(particle.x, particle.y, particle.z)) continue;
            try {
                particle.extract(this.particleTypeRenderState, camera, partialTickTime);
            }
            catch (Throwable throwable) {
                CrashReport report = CrashReport.forThrowable(throwable, "Rendering Particle");
                CrashReportCategory category = report.addCategory("Particle being rendered");
                category.setDetail("Particle", particle::toString);
                category.setDetail("Particle Type", this.particleType::toString);
                throw new ReportedException(report);
            }
        }
        return this.particleTypeRenderState;
    }
}

