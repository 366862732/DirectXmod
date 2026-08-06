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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class NoteParticle
extends SingleQuadParticle {
    private NoteParticle(ClientLevel level, double x, double y, double z, double color, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.friction = 0.66f;
        this.speedUpWhenYMotionIsBlocked = true;
        this.xd *= (double)0.01f;
        this.yd *= (double)0.01f;
        this.zd *= (double)0.01f;
        this.yd += 0.2;
        this.rCol = Math.max(0.0f, Mth.sin(((float)color + 0.0f) * ((float)Math.PI * 2)) * 0.65f + 0.35f);
        this.gCol = Math.max(0.0f, Mth.sin(((float)color + 0.33333334f) * ((float)Math.PI * 2)) * 0.65f + 0.35f);
        this.bCol = Math.max(0.0f, Mth.sin(((float)color + 0.6666667f) * ((float)Math.PI * 2)) * 0.65f + 0.35f);
        this.quadSize *= 1.5f;
        this.lifetime = 6;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float a) {
        return this.quadSize * Mth.clamp(((float)this.age + a) / (float)this.lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            NoteParticle particle = new NoteParticle(level, x, y, z, xAux, this.sprite.get(random));
            return particle;
        }
    }
}

