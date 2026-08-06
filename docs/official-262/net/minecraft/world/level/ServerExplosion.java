/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ServerExplosion
implements Explosion {
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
    private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
    private static final float LARGE_EXPLOSION_RADIUS = 2.0f;
    private final boolean fire;
    private final Explosion.BlockInteraction blockInteraction;
    private final ServerLevel level;
    private final Vec3 center;
    private final @Nullable Entity source;
    private final float radius;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;
    private final Map<Player, Vec3> hitPlayers = new HashMap<Player, Vec3>();

    public ServerExplosion(ServerLevel level, @Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, Vec3 center, float radius, boolean fire, Explosion.BlockInteraction blockInteraction) {
        this.level = level;
        this.source = source;
        this.radius = radius;
        this.center = center;
        this.fire = fire;
        this.blockInteraction = blockInteraction;
        this.damageSource = damageSource == null ? level.damageSources().explosion(this) : damageSource;
        this.damageCalculator = damageCalculator == null ? this.makeDamageCalculator(source) : damageCalculator;
    }

    private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity source) {
        return source == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(source);
    }

    public static float getSeenPercent(Vec3 center, Entity entity) {
        AABB bb = entity.getBoundingBox();
        double xs = 1.0 / ((bb.maxX - bb.minX) * 2.0 + 1.0);
        double ys = 1.0 / ((bb.maxY - bb.minY) * 2.0 + 1.0);
        double zs = 1.0 / ((bb.maxZ - bb.minZ) * 2.0 + 1.0);
        double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
        if (xs < 0.0 || ys < 0.0 || zs < 0.0) {
            return 0.0f;
        }
        int hits = 0;
        int count = 0;
        for (double xx = 0.0; xx <= 1.0; xx += xs) {
            for (double yy = 0.0; yy <= 1.0; yy += ys) {
                for (double zz = 0.0; zz <= 1.0; zz += zs) {
                    double x = Mth.lerp(xx, bb.minX, bb.maxX);
                    double y = Mth.lerp(yy, bb.minY, bb.maxY);
                    double z = Mth.lerp(zz, bb.minZ, bb.maxZ);
                    Vec3 from = new Vec3(x + xOffset, y, z + zOffset);
                    if (entity.level().clip(new ClipContext(from, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS) {
                        ++hits;
                    }
                    ++count;
                }
            }
        }
        return (float)hits / (float)count;
    }

    @Override
    public float radius() {
        return this.radius;
    }

    @Override
    public Vec3 center() {
        return this.center;
    }

    private List<BlockPos> calculateExplodedPositions() {
        HashSet<BlockPos> toBlowSet = new HashSet<BlockPos>();
        int size = 16;
        for (int xx = 0; xx < 16; ++xx) {
            for (int yy = 0; yy < 16; ++yy) {
                block2: for (int zz = 0; zz < 16; ++zz) {
                    if (xx != 0 && xx != 15 && yy != 0 && yy != 15 && zz != 0 && zz != 15) continue;
                    double xd = (float)xx / 15.0f * 2.0f - 1.0f;
                    double yd = (float)yy / 15.0f * 2.0f - 1.0f;
                    double zd = (float)zz / 15.0f * 2.0f - 1.0f;
                    double d = Math.sqrt(xd * xd + yd * yd + zd * zd);
                    xd /= d;
                    yd /= d;
                    zd /= d;
                    double xp = this.center.x;
                    double yp = this.center.y;
                    double zp = this.center.z;
                    float stepSize = 0.3f;
                    for (float remainingPower = this.radius * (0.7f + this.level.random.nextFloat() * 0.6f); remainingPower > 0.0f; remainingPower -= 0.22500001f) {
                        BlockPos pos = BlockPos.containing(xp, yp, zp);
                        BlockState block = this.level.getBlockState(pos);
                        FluidState fluid = this.level.getFluidState(pos);
                        if (!this.level.isInWorldBounds(pos)) continue block2;
                        Optional<Float> resistance = this.damageCalculator.getBlockExplosionResistance(this, this.level, pos, block, fluid);
                        if (resistance.isPresent()) {
                            remainingPower -= (resistance.get().floatValue() + 0.3f) * 0.3f;
                        }
                        if (remainingPower > 0.0f && this.damageCalculator.shouldBlockExplode(this, this.level, pos, block, remainingPower)) {
                            toBlowSet.add(pos);
                        }
                        xp += xd * (double)0.3f;
                        yp += yd * (double)0.3f;
                        zp += zd * (double)0.3f;
                    }
                }
            }
        }
        return new ObjectArrayList(toBlowSet);
    }

    private void hurtEntities() {
        if (this.radius < 1.0E-5f) {
            return;
        }
        float doubleRadius = this.radius * 2.0f;
        int x0 = Mth.floor(this.center.x - (double)doubleRadius - 1.0);
        int x1 = Mth.floor(this.center.x + (double)doubleRadius + 1.0);
        int y0 = Mth.floor(this.center.y - (double)doubleRadius - 1.0);
        int y1 = Mth.floor(this.center.y + (double)doubleRadius + 1.0);
        int z0 = Mth.floor(this.center.z - (double)doubleRadius - 1.0);
        int z1 = Mth.floor(this.center.z + (double)doubleRadius + 1.0);
        List<Entity> entities = this.level.getEntities(this.source, new AABB(x0, y0, z0, x1, y1, z1));
        for (Entity entity : entities) {
            Player player;
            double d;
            float exposure;
            double dist;
            if (entity.ignoreExplosion(this) || (dist = Math.sqrt(entity.distanceToSqr(this.center)) / (double)doubleRadius) > 1.0) continue;
            Vec3 entityOrigin = entity instanceof PrimedTnt ? entity.position() : entity.getEyePosition();
            Vec3 direction = entityOrigin.subtract(this.center).normalize();
            boolean shouldDamageEntity = this.damageCalculator.shouldDamageEntity(this, entity);
            float knockbackMultiplier = this.damageCalculator.getKnockbackMultiplier(entity);
            float f = exposure = shouldDamageEntity || knockbackMultiplier != 0.0f ? ServerExplosion.getSeenPercent(this.center, entity) : 0.0f;
            if (shouldDamageEntity) {
                entity.hurtServer(this.level, this.damageSource, this.damageCalculator.getEntityDamageAmount(this, entity, exposure));
            }
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                d = livingEntity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            } else {
                d = 0.0;
            }
            double knockbackResistance = d;
            double knockbackPower = (1.0 - dist) * (double)exposure * (double)knockbackMultiplier * (1.0 - knockbackResistance);
            Vec3 knockback = direction.scale(knockbackPower);
            entity.push(knockback);
            if (entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile) {
                Projectile projectile = (Projectile)entity;
                projectile.setOwner(this.damageSource.getEntity());
            } else if (!(!(entity instanceof Player) || (player = (Player)entity).isSpectator() || player.isCreative() && player.getAbilities().flying)) {
                this.hitPlayers.put(player, knockback);
            }
            entity.onExplosionHit(this.source);
        }
    }

    private void interactWithBlocks(List<BlockPos> targetBlocks) {
        ArrayList stacks = new ArrayList();
        Util.shuffle(targetBlocks, this.level.random);
        for (BlockPos pos : targetBlocks) {
            this.level.getBlockState(pos).onExplosionHit(this.level, pos, this, (stack, position) -> ServerExplosion.addOrAppendStack(stacks, stack, position));
        }
        for (StackCollector stack2 : stacks) {
            Block.popResource((Level)this.level, stack2.pos, stack2.stack);
        }
    }

    private void createFire(List<BlockPos> targetBlocks) {
        for (BlockPos pos : targetBlocks) {
            if (this.level.random.nextInt(3) != 0 || !this.level.getBlockState(pos).isAir() || !this.level.getBlockState(pos.below()).isSolidRender()) continue;
            this.level.setBlockAndUpdate(pos, BaseFireBlock.getState(this.level, pos));
        }
    }

    public int explode() {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, this.center);
        List<BlockPos> toBlow = this.calculateExplodedPositions();
        this.hurtEntities();
        if (this.interactsWithBlocks()) {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("explosion_blocks");
            this.interactWithBlocks(toBlow);
            profiler.pop();
        }
        if (this.fire) {
            this.createFire(toBlow);
        }
        return toBlow.size();
    }

    private static void addOrAppendStack(List<StackCollector> stacks, ItemStack stack, BlockPos pos) {
        for (StackCollector stackCollector : stacks) {
            stackCollector.tryMerge(stack);
            if (!stack.isEmpty()) continue;
            return;
        }
        stacks.add(new StackCollector(pos, stack));
    }

    private boolean interactsWithBlocks() {
        return this.blockInteraction != Explosion.BlockInteraction.KEEP;
    }

    public Map<Player, Vec3> getHitPlayers() {
        return this.hitPlayers;
    }

    @Override
    public ServerLevel level() {
        return this.level;
    }

    @Override
    public @Nullable LivingEntity getIndirectSourceEntity() {
        return Explosion.getIndirectSourceEntity(this.source);
    }

    @Override
    public @Nullable Entity getDirectSourceEntity() {
        return this.source;
    }

    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    @Override
    public Explosion.BlockInteraction getBlockInteraction() {
        return this.blockInteraction;
    }

    @Override
    public boolean canTriggerBlocks() {
        if (this.blockInteraction != Explosion.BlockInteraction.TRIGGER_BLOCK) {
            return false;
        }
        if (this.source != null && this.source.is(EntityTypes.BREEZE_WIND_CHARGE)) {
            return this.level.getGameRules().get(GameRules.MOB_GRIEFING);
        }
        return true;
    }

    @Override
    public boolean shouldAffectBlocklikeEntities() {
        boolean isNotWindCharge;
        boolean mobGriefingEnabled = this.level.getGameRules().get(GameRules.MOB_GRIEFING);
        boolean bl = isNotWindCharge = this.source == null || !this.source.is(EntityTypes.BREEZE_WIND_CHARGE) && !this.source.is(EntityTypes.WIND_CHARGE);
        if (mobGriefingEnabled) {
            return isNotWindCharge;
        }
        return this.blockInteraction.shouldAffectBlocklikeEntities() && isNotWindCharge;
    }

    public boolean isSmall() {
        return this.radius < 2.0f || !this.interactsWithBlocks();
    }

    private static class StackCollector {
        private final BlockPos pos;
        private ItemStack stack;

        private StackCollector(BlockPos pos, ItemStack stack) {
            this.pos = pos;
            this.stack = stack;
        }

        public void tryMerge(ItemStack input) {
            if (ItemEntity.areMergable(this.stack, input)) {
                this.stack = ItemEntity.merge(this.stack, input, 16);
            }
        }
    }
}

