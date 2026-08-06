/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import java.lang.runtime.SwitchBootstraps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface Explosion {
    public static DamageSource getDefaultDamageSource(Level level, @Nullable Entity source) {
        return level.damageSources().explosion(source, Explosion.getIndirectSourceEntity(source));
    }

    public static @Nullable LivingEntity getIndirectSourceEntity(@Nullable Entity source) {
        LivingEntity livingEntity;
        Entity entity = source;
        int n = 0;
        block5: while (true) {
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{PrimedTnt.class, LivingEntity.class, Projectile.class}, (Entity)entity, n)) {
                case 0: {
                    PrimedTnt primedTnt = (PrimedTnt)entity;
                    livingEntity = primedTnt.getOwner();
                    break block5;
                }
                case 1: {
                    LivingEntity livingEntity2;
                    livingEntity = livingEntity2 = (LivingEntity)entity;
                    break block5;
                }
                case 2: {
                    Projectile projectile = (Projectile)entity;
                    Entity entity2 = projectile.getOwner();
                    if (!(entity2 instanceof LivingEntity)) {
                        n = 3;
                        continue block5;
                    }
                    LivingEntity livingEntity3 = (LivingEntity)entity2;
                    livingEntity = livingEntity3;
                    break block5;
                }
                default: {
                    livingEntity = null;
                    break block5;
                }
            }
            break;
        }
        return livingEntity;
    }

    public ServerLevel level();

    public BlockInteraction getBlockInteraction();

    public @Nullable LivingEntity getIndirectSourceEntity();

    public @Nullable Entity getDirectSourceEntity();

    public float radius();

    public Vec3 center();

    public boolean canTriggerBlocks();

    public boolean shouldAffectBlocklikeEntities();

    public static enum BlockInteraction {
        KEEP(false),
        DESTROY(true),
        DESTROY_WITH_DECAY(true),
        TRIGGER_BLOCK(false);

        private final boolean shouldAffectBlocklikeEntities;

        private BlockInteraction(boolean shouldAffectBlocklikeEntities) {
            this.shouldAffectBlocklikeEntities = shouldAffectBlocklikeEntities;
        }

        public boolean shouldAffectBlocklikeEntities() {
            return this.shouldAffectBlocklikeEntities;
        }
    }
}

