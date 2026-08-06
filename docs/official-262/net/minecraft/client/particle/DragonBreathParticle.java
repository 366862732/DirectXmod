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
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class DragonBreathParticle
extends SingleQuadParticle {
    private static final int COLOR_MIN = 11993298;
    private static final int COLOR_MAX = 14614777;
    private static final float COLOR_MIN_RED = 0.7176471f;
    private static final float COLOR_MIN_GREEN = 0.0f;
    private static final float COLOR_MIN_BLUE = 0.8235294f;
    private static final float COLOR_MAX_RED = 0.8745098f;
    private static final float COLOR_MAX_GREEN = 0.0f;
    private static final float COLOR_MAX_BLUE = 0.9764706f;
    private boolean hasHitGround;
    private final SpriteSet sprites;

    private DragonBreathParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.friction = 0.96f;
        this.xd = xa;
        this.yd = ya;
        this.zd = za;
        this.rCol = Mth.nextFloat(this.random, 0.7176471f, 0.8745098f);
        this.gCol = Mth.nextFloat(this.random, 0.0f, 0.0f);
        this.bCol = Mth.nextFloat(this.random, 0.8235294f, 0.9764706f);
        this.quadSize *= 0.75f;
        this.lifetime = (int)(20.0 / ((double)this.random.nextFloat() * 0.8 + 0.2));
        this.hasHitGround = false;
        this.hasPhysics = false;
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSpriteFromAge(this.sprites);
        if (this.onGround) {
            this.yd = 0.0;
            this.hasHitGround = true;
        }
        if (this.hasHitGround) {
            this.yd += 0.002;
        }
        this.move(this.xd, this.yd, this.zd);
        if (this.y == this.yo) {
            this.xd *= 1.1;
            this.zd *= 1.1;
        }
        this.xd *= (double)this.friction;
        this.zd *= (double)this.friction;
        if (this.hasHitGround) {
            this.yd *= (double)this.friction;
        }
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
    implements ParticleProvider<PowerParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(PowerParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DragonBreathParticle particle = new DragonBreathParticle(level, x, y, z, xAux, yAux, zAux, this.sprites);
            particle.setPower(options.getPower());
            return particle;
        }
    }
}

