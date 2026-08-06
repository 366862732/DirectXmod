/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class DustColorTransitionParticle
extends DustParticleBase<DustColorTransitionOptions> {
    private final Vector3f fromColor;
    private final Vector3f toColor;

    protected DustColorTransitionParticle(ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, DustColorTransitionOptions options, SpriteSet sprites) {
        super(level, x, y, z, xAux, yAux, zAux, options, sprites);
        float baseFactor = this.random.nextFloat() * 0.4f + 0.6f;
        this.fromColor = this.randomizeColor(options.getFromColor(), baseFactor);
        this.toColor = this.randomizeColor(options.getToColor(), baseFactor);
    }

    private Vector3f randomizeColor(Vector3f color, float baseFactor) {
        return new Vector3f(this.randomizeColor(color.x(), baseFactor), this.randomizeColor(color.y(), baseFactor), this.randomizeColor(color.z(), baseFactor));
    }

    private void lerpColors(float partialTickTime) {
        float a = ((float)this.age + partialTickTime) / ((float)this.lifetime + 1.0f);
        Vector3f lerpedColor = new Vector3f((Vector3fc)this.fromColor).lerp((Vector3fc)this.toColor, a);
        this.rCol = lerpedColor.x();
        this.gCol = lerpedColor.y();
        this.bCol = lerpedColor.z();
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        this.lerpColors(partialTickTime);
        super.extract(particleTypeRenderState, camera, partialTickTime);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<DustColorTransitionOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DustColorTransitionOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new DustColorTransitionParticle(level, x, y, z, xAux, yAux, zAux, options, this.sprites);
        }
    }
}

