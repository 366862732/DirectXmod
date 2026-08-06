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
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class TrackingEmitter
extends NoRenderParticle {
    private final Entity entity;
    private int life;
    private final int lifeTime;
    private final ParticleOptions particleType;

    public TrackingEmitter(ClientLevel level, Entity entity, ParticleOptions particleType) {
        this(level, entity, particleType, 3);
    }

    public TrackingEmitter(ClientLevel level, Entity entity, ParticleOptions particleType, int lifeTime) {
        this(level, entity, particleType, lifeTime, entity.getDeltaMovement());
    }

    private TrackingEmitter(ClientLevel level, Entity entity, ParticleOptions particleType, int lifeTime, Vec3 movement) {
        super(level, entity.getX(), entity.getY(0.5), entity.getZ(), movement.x, movement.y, movement.z);
        this.entity = entity;
        this.lifeTime = lifeTime;
        this.particleType = particleType;
        this.tick();
    }

    @Override
    public void tick() {
        for (int i = 0; i < 16; ++i) {
            double za;
            double ya;
            double xa = this.random.nextFloat() * 2.0f - 1.0f;
            if (xa * xa + (ya = (double)(this.random.nextFloat() * 2.0f - 1.0f)) * ya + (za = (double)(this.random.nextFloat() * 2.0f - 1.0f)) * za > 1.0) continue;
            double x = this.entity.getX(xa / 4.0);
            double y = this.entity.getY(0.5 + ya / 4.0);
            double z = this.entity.getZ(za / 4.0);
            this.level.addParticle(this.particleType, x, y, z, xa, ya + 0.2, za);
        }
        ++this.life;
        if (this.life >= this.lifeTime) {
            this.remove();
        }
    }
}

