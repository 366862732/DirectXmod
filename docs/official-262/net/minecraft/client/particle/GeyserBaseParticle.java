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
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.GeyserBaseParticleOptions;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class GeyserBaseParticle
extends BaseAshSmokeParticle {
    private static final float BURST_IMPULSE_FACTOR = 0.25f;
    private static final float PARTICLE_SIZE_BASE = 3.0f;
    private static final float PARTICLE_SIZE_FACTOR = 0.125f;
    private static final float SPAWN_OFFSET_Y = 0.2f;
    private static final float RANDOM_SPAWN_SPREAD = 0.5f;
    private static final int MAX_LIFETIME = 25;
    private static final float FRICTION = 0.725f;

    private GeyserBaseParticle(ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, int waterBlocks, float burstImpulseBase, SpriteSet sprites) {
        float burstImpulse = burstImpulseBase + 0.25f * (float)waterBlocks;
        float size = 3.0f + 0.125f * (float)waterBlocks;
        super(level, x, y, z, burstImpulse, burstImpulse, burstImpulse, xAux, yAux, zAux, size, sprites, 0.0f, 0, 0.0f, true);
        this.friction = 0.725f;
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.yd = Math.abs(this.yd);
        float lifetimeFactor = 0.8f + 0.2f * level.getRandom().nextFloat();
        this.lifetime = (int)(25.0f * lifetimeFactor);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<GeyserBaseParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(GeyserBaseParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            double randomX = x + (double)((random.nextFloat() - 0.5f) * 0.5f);
            double randomY = y + (double)((random.nextFloat() - 0.5f) * 0.5f) + (double)0.2f;
            double randomZ = z + (double)((random.nextFloat() - 0.5f) * 0.5f);
            return new GeyserBaseParticle(level, randomX, randomY, randomZ, xAux, yAux, zAux, options.waterBlocks(), options.burstImpulseBase(), this.sprites);
        }
    }
}

