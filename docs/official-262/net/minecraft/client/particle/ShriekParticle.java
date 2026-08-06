/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public class ShriekParticle
extends SingleQuadParticle {
    private static final float MAGICAL_X_ROT = 1.0472f;
    private int delay;

    private ShriekParticle(ClientLevel level, double x, double y, double z, int delay, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.quadSize = 0.85f;
        this.delay = delay;
        this.lifetime = 30;
        this.gravity = 0.0f;
        this.xd = 0.0;
        this.yd = 0.1;
        this.zd = 0.0;
    }

    @Override
    public float getQuadSize(float a) {
        return this.quadSize * Mth.clamp(((float)this.age + a) / (float)this.lifetime * 0.75f, 0.0f, 1.0f);
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        if (this.delay > 0) {
            return;
        }
        this.alpha = 1.0f - Mth.clamp(((float)this.age + partialTickTime) / (float)this.lifetime, 0.0f, 1.0f);
        Quaternionf rotation = new Quaternionf();
        rotation.rotationX(-1.0472f);
        this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
        rotation.rotationYXZ((float)(-Math.PI), 1.0472f, 0.0f);
        this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
    }

    @Override
    public int getLightCoords(float a) {
        return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        if (this.delay > 0) {
            --this.delay;
            return;
        }
        super.tick();
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<ShriekParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(ShriekParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            ShriekParticle particle = new ShriekParticle(level, x, y, z, options.getDelay(), this.sprite.get(random));
            particle.setAlpha(1.0f);
            return particle;
        }
    }
}

