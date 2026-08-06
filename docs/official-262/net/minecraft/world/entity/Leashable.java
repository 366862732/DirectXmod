/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface Leashable {
    public static final String LEASH_TAG = "leash";
    public static final double LEASH_TOO_FAR_DIST = 12.0;
    public static final double LEASH_ELASTIC_DIST = 6.0;
    public static final double MAXIMUM_ALLOWED_LEASHED_DIST = 16.0;
    public static final Vec3 AXIS_SPECIFIC_ELASTICITY = new Vec3(0.8, 0.2, 0.8);
    public static final float SPRING_DAMPENING = 0.7f;
    public static final double TORSIONAL_ELASTICITY = 10.0;
    public static final double STIFFNESS = 0.11;
    public static final List<Vec3> ENTITY_ATTACHMENT_POINT = ImmutableList.of((Object)new Vec3(0.0, 0.5, 0.5));
    public static final List<Vec3> LEASHER_ATTACHMENT_POINT = ImmutableList.of((Object)new Vec3(0.0, 0.5, 0.0));
    public static final List<Vec3> SHARED_QUAD_ATTACHMENT_POINTS = ImmutableList.of((Object)new Vec3(-0.5, 0.5, 0.5), (Object)new Vec3(-0.5, 0.5, -0.5), (Object)new Vec3(0.5, 0.5, -0.5), (Object)new Vec3(0.5, 0.5, 0.5));

    public @Nullable LeashData getLeashData();

    public void setLeashData(@Nullable LeashData var1);

    default public boolean isLeashed() {
        return this.getLeashData() != null && this.getLeashData().leashHolder != null;
    }

    default public boolean mayBeLeashed() {
        return this.getLeashData() != null;
    }

    default public boolean canHaveALeashAttachedTo(Entity entity) {
        if (this == entity) {
            return false;
        }
        if (this.leashDistanceTo(entity) > this.leashSnapDistance()) {
            return false;
        }
        return this.canBeLeashed();
    }

    default public double leashDistanceTo(Entity entity) {
        return entity.getBoundingBox().getCenter().distanceTo(((Entity)((Object)this)).getBoundingBox().getCenter());
    }

    default public boolean canBeLeashed() {
        return true;
    }

    default public void setDelayedLeashHolderId(int entityId) {
        this.setLeashData(new LeashData(entityId));
        Leashable.dropLeash((Entity)((Object)this), false, false);
    }

    default public void readLeashData(ValueInput input) {
        LeashData newLeashData = input.read(LEASH_TAG, LeashData.CODEC).orElse(null);
        if (this.getLeashData() != null && newLeashData == null) {
            this.removeLeash();
        }
        this.setLeashData(newLeashData);
    }

    default public void writeLeashData(ValueOutput output, @Nullable LeashData leashData) {
        output.storeNullable(LEASH_TAG, LeashData.CODEC, leashData);
    }

    private static <E extends Entity> void restoreLeashFromSave(E entity, LeashData leashData) {
        Level level;
        if (leashData.delayedLeashInfo != null && (level = entity.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Optional leashUuid = leashData.delayedLeashInfo.left();
            Optional pos = leashData.delayedLeashInfo.right();
            if (leashUuid.isPresent()) {
                Entity leasher = serverLevel.getEntity((UUID)leashUuid.get());
                if (leasher != null) {
                    Leashable.setLeashedTo(entity, leasher, true);
                    return;
                }
            } else if (pos.isPresent()) {
                Leashable.setLeashedTo(entity, LeashFenceKnotEntity.getOrCreateKnot(serverLevel, (BlockPos)pos.get()), true);
                return;
            }
            if (entity.tickCount > 100) {
                entity.spawnAtLocation(serverLevel, Items.LEAD);
                ((Leashable)((Object)entity)).setLeashData(null);
            }
        }
    }

    default public void dropLeash() {
        Leashable.dropLeash((Entity)((Object)this), true, true);
    }

    default public void removeLeash() {
        Leashable.dropLeash((Entity)((Object)this), true, false);
    }

    default public void onLeashRemoved() {
    }

    private static <E extends Entity> void dropLeash(E entity, boolean sendPacket, boolean dropLead) {
        LeashData leashData = ((Leashable)((Object)entity)).getLeashData();
        if (leashData != null && leashData.leashHolder != null) {
            ((Leashable)((Object)entity)).setLeashData(null);
            ((Leashable)((Object)entity)).onLeashRemoved();
            Level level = entity.level();
            if (level instanceof ServerLevel) {
                ServerLevel level2 = (ServerLevel)level;
                if (dropLead) {
                    entity.spawnAtLocation(level2, Items.LEAD);
                }
                if (sendPacket) {
                    level2.getChunkSource().sendToTrackingPlayers(entity, new ClientboundSetEntityLinkPacket(entity, null));
                }
                leashData.leashHolder.notifyLeasheeRemoved((Leashable)((Object)entity));
            }
        }
    }

    public static <E extends Entity> void tickLeash(ServerLevel level, E entity) {
        Entity leashHolder;
        LeashData leashData = ((Leashable)((Object)entity)).getLeashData();
        if (leashData != null && leashData.delayedLeashInfo != null) {
            Leashable.restoreLeashFromSave(entity, leashData);
        }
        if (leashData == null || leashData.leashHolder == null) {
            return;
        }
        if (!entity.canInteractWithLevel() || !leashData.leashHolder.canInteractWithLevel()) {
            if (level.getGameRules().get(GameRules.ENTITY_DROPS).booleanValue()) {
                ((Leashable)((Object)entity)).dropLeash();
            } else {
                ((Leashable)((Object)entity)).removeLeash();
            }
        }
        if ((leashHolder = ((Leashable)((Object)entity)).getLeashHolder()) != null && leashHolder.level() == entity.level()) {
            double distanceTo = ((Leashable)((Object)entity)).leashDistanceTo(leashHolder);
            ((Leashable)((Object)entity)).whenLeashedTo(leashHolder);
            if (distanceTo > ((Leashable)((Object)entity)).leashSnapDistance()) {
                level.playSound(null, leashHolder.getX(), leashHolder.getY(), leashHolder.getZ(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f);
                ((Leashable)((Object)entity)).leashTooFarBehaviour();
            } else if (distanceTo > ((Leashable)((Object)entity)).leashElasticDistance() - (double)leashHolder.getBbWidth() - (double)entity.getBbWidth() && ((Leashable)((Object)entity)).checkElasticInteractions(leashHolder, leashData)) {
                ((Leashable)((Object)entity)).onElasticLeashPull();
            } else {
                ((Leashable)((Object)entity)).closeRangeLeashBehaviour(leashHolder);
            }
            entity.setYRot((float)((double)entity.getYRot() - leashData.angularMomentum));
            leashData.angularMomentum *= (double)Leashable.angularFriction(entity);
        }
    }

    default public void onElasticLeashPull() {
        Entity entity = (Entity)((Object)this);
        entity.checkFallDistanceAccumulation();
    }

    default public double leashSnapDistance() {
        return 12.0;
    }

    default public double leashElasticDistance() {
        return 6.0;
    }

    public static <E extends Entity> float angularFriction(E entity) {
        if (entity.onGround()) {
            return entity.level().getBlockState(entity.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91f;
        }
        if (entity.isInLiquid()) {
            return 0.8f;
        }
        return 0.91f;
    }

    default public void whenLeashedTo(Entity leashHolder) {
        leashHolder.notifyLeashHolder(this);
    }

    default public void leashTooFarBehaviour() {
        this.dropLeash();
    }

    default public void closeRangeLeashBehaviour(Entity leashHolder) {
    }

    default public boolean checkElasticInteractions(Entity leashHolder, LeashData leashData) {
        boolean quadConnection = leashHolder.supportQuadLeashAsHolder() && this.supportQuadLeash();
        List<Wrench> wrenches = Leashable.computeElasticInteraction((Entity)((Object)this), leashHolder, quadConnection ? SHARED_QUAD_ATTACHMENT_POINTS : ENTITY_ATTACHMENT_POINT, quadConnection ? SHARED_QUAD_ATTACHMENT_POINTS : LEASHER_ATTACHMENT_POINT);
        if (wrenches.isEmpty()) {
            return false;
        }
        Wrench result = Wrench.accumulate(wrenches).scale(quadConnection ? 0.25 : 1.0);
        leashData.angularMomentum += 10.0 * result.torque();
        Vec3 relativeVelocityToLeasher = Leashable.getHolderMovement(leashHolder).subtract(((Entity)((Object)this)).getKnownMovement());
        ((Entity)((Object)this)).addDeltaMovement(result.force().multiply(AXIS_SPECIFIC_ELASTICITY).add(relativeVelocityToLeasher.scale(0.11)));
        return true;
    }

    private static Vec3 getHolderMovement(Entity leashHolder) {
        Mob mob;
        if (leashHolder instanceof Mob && (mob = (Mob)leashHolder).isNoAi()) {
            return Vec3.ZERO;
        }
        return leashHolder.getKnownMovement();
    }

    private static <E extends Entity> List<Wrench> computeElasticInteraction(E entity, Entity leashHolder, List<Vec3> entityAttachmentPoints, List<Vec3> leasherAttachmentPoints) {
        double slackDistance = ((Leashable)((Object)entity)).leashElasticDistance();
        Vec3 currentMovement = Leashable.getHolderMovement(entity);
        float entityYRot = entity.getYRot() * ((float)Math.PI / 180);
        Vec3 entityDimensions = new Vec3(entity.getBbWidth(), entity.getBbHeight(), entity.getBbWidth());
        float leashHolderYRot = leashHolder.getYRot() * ((float)Math.PI / 180);
        Vec3 leasherDimensions = new Vec3(leashHolder.getBbWidth(), leashHolder.getBbHeight(), leashHolder.getBbWidth());
        ArrayList<Wrench> wrenches = new ArrayList<Wrench>();
        for (int i = 0; i < entityAttachmentPoints.size(); ++i) {
            Vec3 entityAttachVector = entityAttachmentPoints.get(i).multiply(entityDimensions).yRot(-entityYRot);
            Vec3 entityAttachPos = entity.position().add(entityAttachVector);
            Vec3 leasherAttachVector = leasherAttachmentPoints.get(i).multiply(leasherDimensions).yRot(-leashHolderYRot);
            Vec3 leasherAttachPos = leashHolder.position().add(leasherAttachVector);
            Leashable.computeDampenedSpringInteraction(leasherAttachPos, entityAttachPos, slackDistance, currentMovement, entityAttachVector).ifPresent(wrenches::add);
        }
        return wrenches;
    }

    private static Optional<Wrench> computeDampenedSpringInteraction(Vec3 pivotPoint, Vec3 objectPosition, double springSlack, Vec3 objectMotion, Vec3 leverArm) {
        boolean sameDirectionToMovement;
        double distance = objectPosition.distanceTo(pivotPoint);
        if (distance < springSlack) {
            return Optional.empty();
        }
        Vec3 displacement = pivotPoint.subtract(objectPosition).normalize().scale(distance - springSlack);
        double torque = Wrench.torqueFromForce(leverArm, displacement);
        boolean bl = sameDirectionToMovement = objectMotion.dot(displacement) >= 0.0;
        if (sameDirectionToMovement) {
            displacement = displacement.scale(0.3f);
        }
        return Optional.of(new Wrench(displacement, torque));
    }

    default public boolean supportQuadLeash() {
        return false;
    }

    default public Vec3[] getQuadLeashOffsets() {
        return Leashable.createQuadLeashOffsets((Entity)((Object)this), 0.0, 0.5, 0.5, 0.5);
    }

    public static Vec3[] createQuadLeashOffsets(Entity entity, double frontOffset, double frontBack, double leftRight, double height) {
        float width = entity.getBbWidth();
        double frontOffsetScaled = frontOffset * (double)width;
        double frontBackScaled = frontBack * (double)width;
        double leftRightScaled = leftRight * (double)width;
        double heightScaled = height * (double)entity.getBbHeight();
        return new Vec3[]{new Vec3(-leftRightScaled, heightScaled, frontBackScaled + frontOffsetScaled), new Vec3(-leftRightScaled, heightScaled, -frontBackScaled + frontOffsetScaled), new Vec3(leftRightScaled, heightScaled, -frontBackScaled + frontOffsetScaled), new Vec3(leftRightScaled, heightScaled, frontBackScaled + frontOffsetScaled)};
    }

    default public Vec3 getLeashOffset(float partialTicks) {
        return this.getLeashOffset();
    }

    default public Vec3 getLeashOffset() {
        Entity entity = (Entity)((Object)this);
        return new Vec3(0.0, entity.getEyeHeight(), entity.getBbWidth() * 0.4f);
    }

    default public void setLeashedTo(Entity holder, boolean synch) {
        if (this == holder) {
            return;
        }
        Leashable.setLeashedTo((Entity)((Object)this), holder, synch);
    }

    private static <E extends Entity> void setLeashedTo(E entity, Entity holder, boolean synch) {
        Level level;
        LeashData leashData = ((Leashable)((Object)entity)).getLeashData();
        if (leashData == null) {
            leashData = new LeashData(holder);
            ((Leashable)((Object)entity)).setLeashData(leashData);
        } else {
            Entity oldHolder = leashData.leashHolder;
            leashData.setLeashHolder(holder);
            if (oldHolder != null && oldHolder != holder) {
                oldHolder.notifyLeasheeRemoved((Leashable)((Object)entity));
            }
        }
        if (synch && (level = entity.level()) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            level2.getChunkSource().sendToTrackingPlayers(entity, new ClientboundSetEntityLinkPacket(entity, holder));
        }
        if (entity.isPassenger()) {
            entity.stopRiding();
        }
    }

    default public @Nullable Entity getLeashHolder() {
        return Leashable.getLeashHolder((Entity)((Object)this));
    }

    private static <E extends Entity> @Nullable Entity getLeashHolder(E entity) {
        LeashData leashData = ((Leashable)((Object)entity)).getLeashData();
        if (leashData == null) {
            return null;
        }
        Entity ntt = entity.level().getEntity(leashData.delayedLeashHolderId);
        if (leashData.delayedLeashHolderId != 0 && entity.level().isClientSide() && ntt != null) {
            leashData.setLeashHolder(ntt);
        }
        return leashData.leashHolder;
    }

    public static List<Leashable> leashableLeashedTo(Entity entity) {
        return Leashable.leashableInArea(entity, l -> l.getLeashHolder() == entity);
    }

    public static List<Leashable> leashableInArea(Entity entity, Predicate<Leashable> test) {
        return Leashable.leashableInArea(entity.level(), entity.getBoundingBox().getCenter(), test);
    }

    public static List<Leashable> leashableInArea(Level level, Vec3 pos, Predicate<Leashable> test) {
        double size = 32.0;
        AABB scanArea = AABB.ofSize(pos, 32.0, 32.0, 32.0);
        return level.getEntitiesOfClass(Entity.class, scanArea, e -> {
            Leashable leashable;
            return e instanceof Leashable && test.test(leashable = (Leashable)((Object)e));
        }).stream().map(Leashable.class::cast).toList();
    }

    public static final class LeashData {
        public static final Codec<LeashData> CODEC = Codec.xor((Codec)UUIDUtil.CODEC.fieldOf("UUID").codec(), BlockPos.CODEC).xmap(LeashData::new, data -> {
            Entity patt0$temp = data.leashHolder;
            if (patt0$temp instanceof LeashFenceKnotEntity) {
                LeashFenceKnotEntity leashKnot = (LeashFenceKnotEntity)patt0$temp;
                return Either.right((Object)leashKnot.getPos());
            }
            if (data.leashHolder != null) {
                return Either.left((Object)data.leashHolder.getUUID());
            }
            return Objects.requireNonNull(data.delayedLeashInfo, "Invalid LeashData had no attachment");
        });
        private int delayedLeashHolderId;
        public @Nullable Entity leashHolder;
        public @Nullable Either<UUID, BlockPos> delayedLeashInfo;
        public double angularMomentum;

        private LeashData(Either<UUID, BlockPos> delayedLeashInfo) {
            this.delayedLeashInfo = delayedLeashInfo;
        }

        private LeashData(Entity entity) {
            this.leashHolder = entity;
        }

        private LeashData(int entityId) {
            this.delayedLeashHolderId = entityId;
        }

        public void setLeashHolder(Entity leashHolder) {
            this.leashHolder = leashHolder;
            this.delayedLeashInfo = null;
            this.delayedLeashHolderId = 0;
        }
    }

    public record Wrench(Vec3 force, double torque) {
        public static final Wrench ZERO = new Wrench(Vec3.ZERO, 0.0);

        public static double torqueFromForce(Vec3 leverArm, Vec3 force) {
            return leverArm.z * force.x - leverArm.x * force.z;
        }

        public static Wrench accumulate(List<Wrench> wrenches) {
            if (wrenches.isEmpty()) {
                return ZERO;
            }
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            double t = 0.0;
            for (Wrench wrench : wrenches) {
                Vec3 force = wrench.force;
                x += force.x;
                y += force.y;
                z += force.z;
                t += wrench.torque;
            }
            return new Wrench(new Vec3(x, y, z), t);
        }

        public Wrench scale(double scale) {
            return new Wrench(this.force.scale(scale), this.torque * scale);
        }
    }
}

