/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.particle;

import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

@Environment(value=EnvType.CLIENT)
public abstract class Particle {
    private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = Mth.square(100.0);
    protected final ClientLevel level;
    protected double xo;
    protected double yo;
    protected double zo;
    protected double x;
    protected double y;
    protected double z;
    protected double xd;
    protected double yd;
    protected double zd;
    private AABB bb = INITIAL_AABB;
    protected boolean onGround;
    protected boolean hasPhysics = true;
    private boolean stoppedByCollision;
    protected boolean removed;
    protected float bbWidth = 0.6f;
    protected float bbHeight = 1.8f;
    protected final RandomSource random = RandomSource.create();
    protected int age;
    protected int lifetime;
    protected float gravity;
    protected float friction = 0.98f;
    protected boolean speedUpWhenYMotionIsBlocked = false;

    protected Particle(ClientLevel level, double x, double y, double z) {
        this.level = level;
        this.setSize(0.2f, 0.2f);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.lifetime = (int)(4.0f / (this.random.nextFloat() * 0.9f + 0.1f));
    }

    public Particle(ClientLevel level, double x, double y, double z, double xa, double ya, double za) {
        this(level, x, y, z);
        this.xd = xa + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.4f);
        this.yd = ya + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.4f);
        this.zd = za + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.4f);
        double speed = (this.random.nextFloat() + this.random.nextFloat() + 1.0f) * 0.15f;
        double dd = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        this.xd = this.xd / dd * speed * (double)0.4f;
        this.yd = this.yd / dd * speed * (double)0.4f + (double)0.1f;
        this.zd = this.zd / dd * speed * (double)0.4f;
    }

    public Particle setPower(float power) {
        this.xd *= (double)power;
        this.yd = (this.yd - (double)0.1f) * (double)power + (double)0.1f;
        this.zd *= (double)power;
        return this;
    }

    public void setParticleSpeed(double xd, double yd, double zd) {
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
    }

    public Particle scale(float scale) {
        this.setSize(0.2f * scale, 0.2f * scale);
        return this;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.yd -= 0.04 * (double)this.gravity;
        this.move(this.xd, this.yd, this.zd);
        if (this.speedUpWhenYMotionIsBlocked && this.y == this.yo) {
            this.xd *= 1.1;
            this.zd *= 1.1;
        }
        this.xd *= (double)this.friction;
        this.yd *= (double)this.friction;
        this.zd *= (double)this.friction;
        if (this.onGround) {
            this.xd *= (double)0.7f;
            this.zd *= (double)0.7f;
        }
    }

    public abstract ParticleRenderType getGroup();

    public String toString() {
        return this.getClass().getSimpleName() + ", Pos (" + this.x + "," + this.y + "," + this.z + "), Age " + this.age;
    }

    public void remove() {
        this.removed = true;
    }

    protected void setSize(float w, float h) {
        if (w != this.bbWidth || h != this.bbHeight) {
            this.bbWidth = w;
            this.bbHeight = h;
            AABB aabb = this.getBoundingBox();
            double newMinX = (aabb.minX + aabb.maxX - (double)w) / 2.0;
            double newMinZ = (aabb.minZ + aabb.maxZ - (double)w) / 2.0;
            this.setBoundingBox(new AABB(newMinX, aabb.minY, newMinZ, newMinX + (double)this.bbWidth, aabb.minY + (double)this.bbHeight, newMinZ + (double)this.bbWidth));
        }
    }

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        float w = this.bbWidth / 2.0f;
        float h = this.bbHeight;
        this.setBoundingBox(new AABB(x - (double)w, y, z - (double)w, x + (double)w, y + (double)h, z + (double)w));
    }

    public void move(double xa, double ya, double za) {
        if (this.stoppedByCollision) {
            return;
        }
        double originalXa = xa;
        double originalYa = ya;
        double originalZa = za;
        if (this.hasPhysics && (xa != 0.0 || ya != 0.0 || za != 0.0) && xa * xa + ya * ya + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            Vec3 movement = Entity.collideBoundingBox(CollisionContext.positionContext(this.y), new Vec3(xa, ya, za), this.getBoundingBox(), (Level)this.level, List.of());
            xa = movement.x;
            ya = movement.y;
            za = movement.z;
        }
        if (xa != 0.0 || ya != 0.0 || za != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
            this.setLocationFromBoundingbox();
        }
        if (Math.abs(originalYa) >= (double)1.0E-5f && Math.abs(ya) < (double)1.0E-5f) {
            this.stoppedByCollision = true;
        }
        boolean bl = this.onGround = originalYa != ya && originalYa < 0.0;
        if (originalXa != xa) {
            this.xd = 0.0;
        }
        if (originalZa != za) {
            this.zd = 0.0;
        }
    }

    protected void setLocationFromBoundingbox() {
        AABB aabb = this.getBoundingBox();
        this.x = (aabb.minX + aabb.maxX) / 2.0;
        this.y = aabb.minY;
        this.z = (aabb.minZ + aabb.maxZ) / 2.0;
    }

    protected int getLightCoords(float a) {
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (this.level.hasChunkAt(pos)) {
            return LightCoordsUtil.getLightCoords(this.level, pos);
        }
        return 0xF00000;
    }

    public boolean isAlive() {
        return !this.removed;
    }

    public AABB getBoundingBox() {
        return this.bb;
    }

    public void setBoundingBox(AABB bb) {
        this.bb = bb;
    }

    public Optional<ParticleLimit> getParticleLimit() {
        return Optional.empty();
    }

    @Environment(value=EnvType.CLIENT)
    public record LifetimeAlpha(float startAlpha, float endAlpha, float startAtNormalizedAge, float endAtNormalizedAge) {
        public static final LifetimeAlpha ALWAYS_OPAQUE = new LifetimeAlpha(1.0f, 1.0f, 0.0f, 1.0f);

        public boolean isOpaque() {
            return this.startAlpha >= 1.0f && this.endAlpha >= 1.0f;
        }

        public float currentAlphaForAge(int age, int lifetime, float partialTickTime) {
            if (Mth.equal(this.startAlpha, this.endAlpha)) {
                return this.startAlpha;
            }
            float timeNormalized = Mth.inverseLerp(((float)age + partialTickTime) / (float)lifetime, this.startAtNormalizedAge, this.endAtNormalizedAge);
            return Mth.clampedLerp(timeNormalized, this.startAlpha, this.endAlpha);
        }
    }
}

