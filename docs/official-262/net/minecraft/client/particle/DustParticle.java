/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

@Environment(value=EnvType.CLIENT)
public class DustParticle
extends DustParticleBase<DustParticleOptions> {
    protected DustParticle(ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, DustParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, xAux, yAux, zAux, options, sprites);
        float baseFactor = this.random.nextFloat() * 0.4f + 0.6f;
        Vector3f color = options.getColor();
        this.rCol = this.randomizeColor(color.x(), baseFactor);
        this.gCol = this.randomizeColor(color.y(), baseFactor);
        this.bCol = this.randomizeColor(color.z(), baseFactor);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<DustParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DustParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new DustParticle(level, x, y, z, xAux, yAux, zAux, options, this.sprites);
        }
    }
}

