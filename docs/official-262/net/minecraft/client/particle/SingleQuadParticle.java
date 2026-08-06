/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.particle;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public abstract class SingleQuadParticle
extends Particle {
    protected float quadSize;
    protected float rCol = 1.0f;
    protected float gCol = 1.0f;
    protected float bCol = 1.0f;
    protected float alpha = 1.0f;
    protected float roll;
    protected float oRoll;
    protected TextureAtlasSprite sprite;

    protected SingleQuadParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        this.sprite = sprite;
        this.quadSize = 0.1f * (this.random.nextFloat() * 0.5f + 0.5f) * 2.0f;
    }

    protected SingleQuadParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, TextureAtlasSprite sprite) {
        super(level, x, y, z, xa, ya, za);
        this.sprite = sprite;
        this.quadSize = 0.1f * (this.random.nextFloat() * 0.5f + 0.5f) * 2.0f;
    }

    public FacingCameraMode getFacingCameraMode() {
        return FacingCameraMode.LOOKAT_XYZ;
    }

    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        Quaternionf rotation = new Quaternionf();
        this.getFacingCameraMode().setRotation(rotation, camera, partialTickTime);
        if (this.roll != 0.0f) {
            rotation.rotateZ(Mth.lerp(partialTickTime, this.oRoll, this.roll));
        }
        this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
    }

    protected void extractRotatedQuad(QuadParticleRenderState particleTypeRenderState, Camera camera, Quaternionf rotation, float partialTickTime) {
        Vec3 pos = camera.position();
        float x = (float)(Mth.lerp((double)partialTickTime, this.xo, this.x) - pos.x());
        float y = (float)(Mth.lerp((double)partialTickTime, this.yo, this.y) - pos.y());
        float z = (float)(Mth.lerp((double)partialTickTime, this.zo, this.z) - pos.z());
        this.extractRotatedQuad(particleTypeRenderState, rotation, x, y, z, partialTickTime);
    }

    protected void extractRotatedQuad(QuadParticleRenderState particleTypeRenderState, Quaternionf rotation, float x, float y, float z, float partialTickTime) {
        particleTypeRenderState.add(this.getLayer(), x, y, z, rotation.x, rotation.y, rotation.z, rotation.w, this.getQuadSize(partialTickTime), this.getU0(), this.getU1(), this.getV0(), this.getV1(), ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol), this.getLightCoords(partialTickTime));
    }

    public float getQuadSize(float a) {
        return this.quadSize;
    }

    @Override
    public Particle scale(float scale) {
        this.quadSize *= scale;
        return super.scale(scale);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    public void setSpriteFromAge(SpriteSet sprites) {
        if (!this.removed) {
            this.setSprite(sprites.get(this.age, this.lifetime));
        }
    }

    protected void setSprite(TextureAtlasSprite icon) {
        this.sprite = icon;
    }

    protected float getU0() {
        return this.sprite.getU0();
    }

    protected float getU1() {
        return this.sprite.getU1();
    }

    protected float getV0() {
        return this.sprite.getV0();
    }

    protected float getV1() {
        return this.sprite.getV1();
    }

    protected abstract Layer getLayer();

    public void setColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    protected void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ", Pos (" + this.x + "," + this.y + "," + this.z + "), RGBA (" + this.rCol + "," + this.gCol + "," + this.bCol + "," + this.alpha + "), Age " + this.age;
    }

    @Environment(value=EnvType.CLIENT)
    public static interface FacingCameraMode {
        public static final FacingCameraMode LOOKAT_XYZ = (target, camera, partialTickTime) -> target.set((Quaternionfc)camera.rotation());
        public static final FacingCameraMode LOOKAT_Y = (target, camera, partialTickTime) -> target.set(0.0f, camera.rotation().y, 0.0f, camera.rotation().w);

        public void setRotation(Quaternionf var1, Camera var2, float var3);
    }

    @Environment(value=EnvType.CLIENT)
    public record Layer(boolean translucent, Identifier textureAtlasLocation, RenderPipeline pipeline) {
        public static final Layer OPAQUE_TERRAIN = new Layer(false, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.OPAQUE_PARTICLE);
        public static final Layer TRANSLUCENT_TERRAIN = new Layer(true, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.TRANSLUCENT_PARTICLE);
        public static final Layer OPAQUE_ITEMS = new Layer(false, TextureAtlas.LOCATION_ITEMS, RenderPipelines.OPAQUE_PARTICLE);
        public static final Layer TRANSLUCENT_ITEMS = new Layer(true, TextureAtlas.LOCATION_ITEMS, RenderPipelines.TRANSLUCENT_PARTICLE);
        public static final Layer OPAQUE = new Layer(false, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.OPAQUE_PARTICLE);
        public static final Layer TRANSLUCENT = new Layer(true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_PARTICLE);

        public static Layer bySprite(TextureAtlasSprite sprite) {
            boolean translucent = sprite.transparency().hasTranslucent();
            if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
                return translucent ? TRANSLUCENT_TERRAIN : OPAQUE_TERRAIN;
            }
            if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_ITEMS)) {
                return translucent ? TRANSLUCENT_ITEMS : OPAQUE_ITEMS;
            }
            return translucent ? TRANSLUCENT : OPAQUE;
        }
    }
}

