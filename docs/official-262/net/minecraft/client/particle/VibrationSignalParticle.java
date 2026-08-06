/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 */
package net.minecraft.client.particle;

import java.util.Optional;
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
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public class VibrationSignalParticle
extends SingleQuadParticle {
    private final PositionSource target;
    private float rot;
    private float rotO;
    private float pitch;
    private float pitchO;

    private VibrationSignalParticle(ClientLevel level, double x, double y, double z, PositionSource target, int arrivalInTicks, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.quadSize = 0.3f;
        this.target = target;
        this.lifetime = arrivalInTicks;
        Optional<Vec3> position = target.getPosition(level);
        if (position.isPresent()) {
            Vec3 destination = position.get();
            double dx = x - destination.x();
            double dy = y - destination.y();
            double dz = z - destination.z();
            this.rotO = this.rot = (float)Mth.atan2(dx, dz);
            this.pitchO = this.pitch = (float)Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        }
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        float randomSway = Mth.sin(((float)this.age + partialTickTime - (float)Math.PI * 2) * 0.05f) * 2.0f;
        float lerpedRotation = Mth.lerp(partialTickTime, this.rotO, this.rot);
        float lerpedPitch = Mth.lerp(partialTickTime, this.pitchO, this.pitch) + 1.5707964f;
        Quaternionf rotation = new Quaternionf();
        rotation.rotationY(lerpedRotation).rotateX(-lerpedPitch).rotateY(randomSway);
        this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
        rotation.rotationY((float)(-Math.PI) + lerpedRotation).rotateX(lerpedPitch).rotateY(randomSway);
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
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        Optional<Vec3> position = this.target.getPosition(this.level);
        if (position.isEmpty()) {
            this.remove();
            return;
        }
        int ticksRemaining = this.lifetime - this.age;
        double alpha = 1.0 / (double)ticksRemaining;
        Vec3 destination = position.get();
        this.x = Mth.lerp(alpha, this.x, destination.x());
        this.y = Mth.lerp(alpha, this.y, destination.y());
        this.z = Mth.lerp(alpha, this.z, destination.z());
        double dx = this.x - destination.x();
        double dy = this.y - destination.y();
        double dz = this.z - destination.z();
        this.rotO = this.rot;
        this.rot = (float)Mth.atan2(dx, dz);
        this.pitchO = this.pitch;
        this.pitch = (float)Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz));
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<VibrationParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(VibrationParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            VibrationSignalParticle particle = new VibrationSignalParticle(level, x, y, z, options.getDestination(), options.getArrivalInTicks(), this.sprite.get(random));
            particle.setAlpha(1.0f);
            return particle;
        }
    }
}

