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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

@Environment(value=EnvType.CLIENT)
public class BreakingItemParticle
extends SingleQuadParticle {
    private final float uo;
    private final float vo;
    private final SingleQuadParticle.Layer layer;

    private BreakingItemParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, TextureAtlasSprite sprite) {
        this(level, x, y, z, sprite);
        this.xd *= (double)0.1f;
        this.yd *= (double)0.1f;
        this.zd *= (double)0.1f;
        this.xd += xa;
        this.yd += ya;
        this.zd += za;
    }

    protected BreakingItemParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.gravity = 1.0f;
        this.quadSize /= 2.0f;
        this.uo = this.random.nextFloat() * 3.0f;
        this.vo = this.random.nextFloat() * 3.0f;
        this.layer = SingleQuadParticle.Layer.bySprite(sprite);
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0f) / 4.0f);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0f);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0f);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0f) / 4.0f);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return this.layer;
    }

    @Environment(value=EnvType.CLIENT)
    public static class SnowballProvider
    extends ItemParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new BreakingItemParticle(level, x, y, z, this.getSprite(new ItemStackTemplate(Items.SNOWBALL), level, random));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class CobwebProvider
    extends ItemParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new BreakingItemParticle(level, x, y, z, this.getSprite(new ItemStackTemplate(Items.COBWEB), level, random));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class SulfurCubeProvider
    extends ItemParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SulfurCubeProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new BreakingItemParticle(level, x, y, z, this.sprites.first());
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class SlimeProvider
    extends ItemParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new BreakingItemParticle(level, x, y, z, this.getSprite(new ItemStackTemplate(Items.SLIME_BALL), level, random));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    extends ItemParticleProvider<ItemParticleOption> {
        @Override
        public Particle createParticle(ItemParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new BreakingItemParticle(level, x, y, z, xAux, yAux, zAux, this.getSprite(options.getItem(), level, random));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class ItemParticleProvider<T extends ParticleOptions>
    implements ParticleProvider<T> {
        private final ItemStackRenderState scratchRenderState = new ItemStackRenderState();

        protected TextureAtlasSprite getSprite(ItemStackTemplate item, ClientLevel level, RandomSource random) {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(this.scratchRenderState, item.create(), ItemDisplayContext.GROUND, level, null, 0);
            Material.Baked material = this.scratchRenderState.pickParticleMaterial(random);
            return material != null ? material.sprite() : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
        }
    }
}

