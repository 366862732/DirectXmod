/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.GeyserBaseParticleOptions;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GeyserEruptionParticle
extends NoRenderParticle {
    public static final int BASE_PARTICLE_FREQUENCY = 2;
    public static final int BASE_PARTICLE_AMOUNT = 2;
    public static final int POOF_PARTICLE_FREQUENCY = 10;
    public static final int POOF_PARTICLE_AMOUNT = 20;
    private static final float BASE_BURST_IMPULSE = 1.5f;
    private static final float POOF_BURST_IMPULSE = 2.0f;
    private final int waterBlocks;
    private final double xa;
    private final double ya;
    private final double za;
    private final GeyserParticleOptions plumeParticle;
    private final GeyserBaseParticleOptions baseParticle;
    private final GeyserBaseParticleOptions poofParticle;

    protected GeyserEruptionParticle(ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, GeyserParticleOptions options) {
        super(level, x, y, z);
        this.xa = xAux;
        this.ya = yAux;
        this.za = zAux;
        this.waterBlocks = options.waterBlocks();
        this.lifetime = 20;
        this.plumeParticle = new GeyserParticleOptions(ParticleTypes.GEYSER_PLUME, this.waterBlocks);
        this.baseParticle = new GeyserBaseParticleOptions(ParticleTypes.GEYSER_BASE, this.waterBlocks, 1.5f);
        this.poofParticle = new GeyserBaseParticleOptions(ParticleTypes.GEYSER_POOF, this.waterBlocks, 2.0f);
    }

    @Override
    public void tick() {
        int i;
        super.tick();
        if (this.age % 2 == 0) {
            for (i = 0; i < 2; ++i) {
                this.level.addParticle(this.baseParticle, this.x, this.y, this.z, this.xa, this.ya, this.za);
            }
        }
        for (i = 0; i < this.waterBlocks + 2; ++i) {
            this.level.addParticle(this.plumeParticle, this.x, this.y, this.z, this.xa, this.ya, this.za);
        }
        if (this.age % 10 == 0) {
            for (i = 0; i < 20; ++i) {
                this.level.addParticle(this.poofParticle, this.x, this.y, this.z, this.xa, this.ya, this.za);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<GeyserParticleOptions> {
        @Override
        public @Nullable Particle createParticle(GeyserParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new GeyserEruptionParticle(level, x, y, z, xAux, yAux, zAux, options);
        }
    }
}

