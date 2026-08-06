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
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GeyserPlumeParticle
extends SingleQuadParticle {
    private static final float NO_FRICTION = 1.0f;
    private static final float MAX_FRICTION = 0.0f;
    private static final float INITIAL_PROPULSION_FACTOR = 1.45f;
    private static final float GRAVITY_EXPONENT = 3.0f;
    private static final float GRADUAL_GRAVITY_FACTOR = 0.12f;
    private static final float INITIAL_SPRAY_SPREAD = 0.2f;
    private static final float LINEAR_SPRAY_SPREAD = 0.2f;
    private static final int REMAINING_FRAMES_AFTER_REACHING_MAX_HEIGHT = 5;
    private static final float MIN_SCALE_FACTOR = 2.0f;
    private static final float MAX_SCALE_FACTOR = 3.0f;
    private final SpriteSet sprites;
    private final double startY;
    private final double maxY;
    private final float initialPropulsion;
    private final float horizontalSprayX;
    private final float horizontalSprayZ;
    private final float minSize;
    private final float maxSize;
    private boolean done;

    private GeyserPlumeParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, GeyserParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, xa, ya, za, sprites.first());
        int plumeHeight = 5 * Math.max(1, options.waterBlocks());
        this.hasPhysics = true;
        this.speedUpWhenYMotionIsBlocked = true;
        this.lifetime = plumeHeight * 5;
        this.yd = 0.0;
        this.startY = y;
        this.maxY = this.startY + (double)plumeHeight - 1.0;
        this.horizontalSprayX = (level.getRandom().nextFloat() - 0.5f) * 0.2f;
        this.horizontalSprayZ = (level.getRandom().nextFloat() - 0.5f) * 0.2f;
        this.friction = 1.0f;
        this.initialPropulsion = (options.waterBlocks() == 1 ? 1.5f : 1.0f) * (float)plumeHeight * 1.45f;
        this.gravity = -this.initialPropulsion;
        float initiallyRandomizedSize = this.quadSize * 0.75f;
        this.minSize = initiallyRandomizedSize * (2.0f + (float)plumeHeight / 8.0f);
        this.maxSize = initiallyRandomizedSize * (3.0f + (float)plumeHeight / 8.0f);
        this.quadSize = this.minSize;
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.done && (this.yd < 0.0 || this.y > this.maxY || this.y == this.yo)) {
            this.lifetime = Math.min(this.lifetime, this.age + 5);
            this.friction = 0.0f;
            this.done = true;
        }
        double yProgressLinear = Math.clamp((this.y - this.startY) / (this.maxY - this.startY), 0.0, 1.0);
        double yProgressExponential = Math.pow(yProgressLinear, 3.0);
        this.gravity = this.initialPropulsion * (float)yProgressExponential * 0.12f;
        this.xd = yProgressLinear * (double)this.horizontalSprayX;
        this.zd = yProgressLinear * (double)this.horizontalSprayZ;
        this.setSpriteFromAge(this.sprites);
        this.quadSize = this.minSize + (float)(yProgressLinear * (double)(this.maxSize - this.minSize));
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<GeyserParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(GeyserParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            double randomX = x + (double)((random.nextFloat() - 0.5f) * 0.2f);
            double randomY = y + (double)random.nextFloat();
            double randomZ = z + (double)((random.nextFloat() - 0.5f) * 0.2f);
            return new GeyserPlumeParticle(level, randomX, randomY, randomZ, xAux, yAux, zAux, options, this.sprites);
        }
    }
}

