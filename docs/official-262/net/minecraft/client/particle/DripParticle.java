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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

@Environment(value=EnvType.CLIENT)
public class DripParticle
extends SingleQuadParticle {
    private final Fluid type;
    protected boolean isGlowing;

    private DripParticle(ClientLevel level, double x, double y, double z, Fluid type, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.setSize(0.01f, 0.01f);
        this.gravity = 0.06f;
        this.type = type;
    }

    protected Fluid getType() {
        return this.type;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public int getLightCoords(float a) {
        if (this.isGlowing) {
            return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
        }
        return super.getLightCoords(a);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.preMoveUpdate();
        if (this.removed) {
            return;
        }
        this.yd -= (double)this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.postMoveUpdate();
        if (this.removed) {
            return;
        }
        this.xd *= (double)0.98f;
        this.yd *= (double)0.98f;
        this.zd *= (double)0.98f;
        if (this.type == Fluids.EMPTY) {
            return;
        }
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        FluidState fluidState = this.level.getFluidState(pos);
        if (fluidState.is(this.type) && this.y < (double)((float)pos.getY() + fluidState.getHeight(this.level, pos))) {
            this.remove();
        }
    }

    protected void preMoveUpdate() {
        if (this.lifetime-- <= 0) {
            this.remove();
        }
    }

    protected void postMoveUpdate() {
    }

    @Environment(value=EnvType.CLIENT)
    public static class ObsidianTearLandProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripLandParticle particle = new DripLandParticle(level, x, y, z, Fluids.EMPTY, this.sprite.get(random));
            particle.isGlowing = true;
            particle.lifetime = (int)(28.0 / ((double)random.nextFloat() * 0.8 + 0.2));
            particle.setColor(0.51171875f, 0.03125f, 0.890625f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ObsidianTearFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FallAndLandParticle particle = new FallAndLandParticle(level, x, y, z, Fluids.EMPTY, ParticleTypes.LANDING_OBSIDIAN_TEAR, this.sprite.get(random));
            particle.isGlowing = true;
            particle.gravity = 0.01f;
            particle.setColor(0.51171875f, 0.03125f, 0.890625f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ObsidianTearHangProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripHangParticle particle = new DripHangParticle(level, x, y, z, Fluids.EMPTY, ParticleTypes.FALLING_OBSIDIAN_TEAR, this.sprite.get(random));
            particle.isGlowing = true;
            particle.gravity *= 0.01f;
            particle.lifetime = 100;
            particle.setColor(0.51171875f, 0.03125f, 0.890625f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class SporeBlossomFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SporeBlossomFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FallingParticle particle = new FallingParticle(level, x, y, z, Fluids.EMPTY, this.sprite.get(random));
            particle.lifetime = (int)(64.0f / Mth.randomBetween(particle.random, 0.1f, 0.9f));
            particle.gravity = 0.005f;
            particle.setColor(0.32f, 0.5f, 0.22f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class NectarFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public NectarFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FallingParticle particle = new FallingParticle(level, x, y, z, Fluids.EMPTY, this.sprite.get(random));
            particle.lifetime = (int)(16.0 / ((double)random.nextFloat() * 0.8 + 0.2));
            particle.gravity = 0.007f;
            particle.setColor(0.92f, 0.782f, 0.72f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class DripstoneLavaFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneLavaFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripstoneFallAndLandParticle particle = new DripstoneFallAndLandParticle(level, x, y, z, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(random));
            particle.setColor(1.0f, 0.2857143f, 0.083333336f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class DripstoneLavaHangProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneLavaHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            CoolingDripHangParticle particle = new CoolingDripHangParticle(level, x, y, z, Fluids.LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA, this.sprite.get(random));
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class DripstoneWaterFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneWaterFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripstoneFallAndLandParticle particle = new DripstoneFallAndLandParticle(level, x, y, z, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(random));
            particle.setColor(0.2f, 0.3f, 1.0f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class DripstoneWaterHangProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneWaterHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripHangParticle particle = new DripHangParticle(level, x, y, z, Fluids.WATER, ParticleTypes.FALLING_DRIPSTONE_WATER, this.sprite.get(random));
            particle.setColor(0.2f, 0.3f, 1.0f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class HoneyLandProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripLandParticle particle = new DripLandParticle(level, x, y, z, Fluids.EMPTY, this.sprite.get(random));
            particle.lifetime = (int)(128.0 / ((double)random.nextFloat() * 0.8 + 0.2));
            particle.setColor(0.522f, 0.408f, 0.082f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class HoneyFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            HoneyFallAndLandParticle particle = new HoneyFallAndLandParticle(level, x, y, z, Fluids.EMPTY, ParticleTypes.LANDING_HONEY, this.sprite.get(random));
            particle.gravity = 0.01f;
            particle.setColor(0.582f, 0.448f, 0.082f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class HoneyHangProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripHangParticle particle = new DripHangParticle(level, x, y, z, Fluids.EMPTY, ParticleTypes.FALLING_HONEY, this.sprite.get(random));
            particle.gravity *= 0.01f;
            particle.lifetime = 100;
            particle.setColor(0.622f, 0.508f, 0.082f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class LavaLandProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripLandParticle particle = new DripLandParticle(level, x, y, z, Fluids.LAVA, this.sprite.get(random));
            particle.setColor(1.0f, 0.2857143f, 0.083333336f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class LavaFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FallAndLandParticle particle = new FallAndLandParticle(level, x, y, z, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(random));
            particle.setColor(1.0f, 0.2857143f, 0.083333336f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class LavaHangProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            CoolingDripHangParticle particle = new CoolingDripHangParticle(level, x, y, z, Fluids.LAVA, ParticleTypes.FALLING_LAVA, this.sprite.get(random));
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class WaterFallProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WaterFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FallAndLandParticle particle = new FallAndLandParticle(level, x, y, z, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(random));
            particle.setColor(0.2f, 0.3f, 1.0f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class WaterHangProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WaterHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            DripHangParticle particle = new DripHangParticle(level, x, y, z, Fluids.WATER, ParticleTypes.FALLING_WATER, this.sprite.get(random));
            particle.setColor(0.2f, 0.3f, 1.0f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class DripLandParticle
    extends DripParticle {
        private DripLandParticle(ClientLevel level, double x, double y, double z, Fluid type, TextureAtlasSprite sprite) {
            super(level, x, y, z, type, sprite);
            this.lifetime = (int)(16.0 / ((double)this.random.nextFloat() * 0.8 + 0.2));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class FallingParticle
    extends DripParticle {
        private FallingParticle(ClientLevel level, double x, double y, double z, Fluid type, TextureAtlasSprite sprite) {
            super(level, x, y, z, type, sprite);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class DripstoneFallAndLandParticle
    extends FallAndLandParticle {
        private DripstoneFallAndLandParticle(ClientLevel level, double x, double y, double z, Fluid type, ParticleOptions landParticle, TextureAtlasSprite sprite) {
            super(level, x, y, z, type, landParticle, sprite);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
                SoundEvent sound = this.getType() == Fluids.LAVA ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
                float volume = Mth.randomBetween(this.random, 0.3f, 1.0f);
                this.level.playLocalSound(this.x, this.y, this.z, sound, SoundSource.BLOCKS, volume, 1.0f, false);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class HoneyFallAndLandParticle
    extends FallAndLandParticle {
        private HoneyFallAndLandParticle(ClientLevel level, double x, double y, double z, Fluid type, ParticleOptions landParticle, TextureAtlasSprite sprite) {
            super(level, x, y, z, type, landParticle, sprite);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
                float volume = Mth.randomBetween(this.random, 0.3f, 1.0f);
                this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, volume, 1.0f, false);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class FallAndLandParticle
    extends FallingParticle {
        protected final ParticleOptions landParticle;

        private FallAndLandParticle(ClientLevel level, double x, double y, double z, Fluid type, ParticleOptions landParticle, TextureAtlasSprite sprite) {
            super(level, x, y, z, type, sprite);
            this.lifetime = (int)(64.0 / ((double)this.random.nextFloat() * 0.8 + 0.2));
            this.landParticle = landParticle;
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class CoolingDripHangParticle
    extends DripHangParticle {
        private CoolingDripHangParticle(ClientLevel level, double x, double y, double z, Fluid type, ParticleOptions fallingParticle, TextureAtlasSprite sprite) {
            super(level, x, y, z, type, fallingParticle, sprite);
        }

        @Override
        protected void preMoveUpdate() {
            this.rCol = 1.0f;
            this.gCol = 16.0f / (float)(40 - this.lifetime + 16);
            this.bCol = 4.0f / (float)(40 - this.lifetime + 8);
            super.preMoveUpdate();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class DripHangParticle
    extends DripParticle {
        private final ParticleOptions fallingParticle;

        private DripHangParticle(ClientLevel level, double x, double y, double z, Fluid type, ParticleOptions fallingParticle, TextureAtlasSprite sprite) {
            super(level, x, y, z, type, sprite);
            this.fallingParticle = fallingParticle;
            this.gravity *= 0.02f;
            this.lifetime = 40;
        }

        @Override
        protected void preMoveUpdate() {
            if (this.lifetime-- <= 0) {
                this.remove();
                this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
            }
        }

        @Override
        protected void postMoveUpdate() {
            this.xd *= 0.02;
            this.yd *= 0.02;
            this.zd *= 0.02;
        }
    }
}

