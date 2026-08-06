/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Predicates
 */
package net.minecraft.world.entity;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

public final class EntitySelector {
    public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
    public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = entity -> entity.isAlive() && entity instanceof LivingEntity;
    public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = entity -> entity.isAlive() && !entity.isVehicle() && !entity.isPassenger();
    public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = entity -> entity instanceof Container && entity.isAlive();
    public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = entity -> {
        if (!(entity instanceof Player)) return true;
        Player player = (Player)entity;
        if (entity.isSpectator()) return false;
        if (player.isCreative()) return false;
        return true;
    };
    public static final Predicate<Entity> NO_SPECTATORS = entity -> !entity.isSpectator();
    public static final Predicate<Entity> CAN_BE_COLLIDED_WITH = NO_SPECTATORS.and(entity -> entity.canBeCollidedWith(null));
    public static final Predicate<Entity> CAN_BE_PICKED = Entity::isPickable;

    private EntitySelector() {
    }

    public static Predicate<Entity> withinDistance(double centerX, double centerY, double centerZ, double distance) {
        double distanceSqr = distance * distance;
        return input -> input.distanceToSqr(centerX, centerY, centerZ) <= distanceSqr;
    }

    public static Predicate<Entity> pushableBy(Entity entity) {
        Team.CollisionRule ownCollisionRule;
        PlayerTeam ownTeam = entity.getTeam();
        Team.CollisionRule collisionRule = ownCollisionRule = ownTeam == null ? Team.CollisionRule.ALWAYS : ((Team)ownTeam).getCollisionRule();
        if (ownCollisionRule == Team.CollisionRule.NEVER) {
            return Predicates.alwaysFalse();
        }
        return NO_SPECTATORS.and(input -> {
            boolean sameTeam;
            Team.CollisionRule theirCollisionRule;
            Player player;
            if (!input.isPushable()) {
                return false;
            }
            if (!(!entity.level().isClientSide() || input instanceof Player && (player = (Player)input).isLocalPlayer())) {
                return false;
            }
            PlayerTeam theirTeam = input.getTeam();
            Team.CollisionRule collisionRule = theirCollisionRule = theirTeam == null ? Team.CollisionRule.ALWAYS : ((Team)theirTeam).getCollisionRule();
            if (theirCollisionRule == Team.CollisionRule.NEVER) {
                return false;
            }
            boolean bl = sameTeam = ownTeam != null && ownTeam.isAlliedTo(theirTeam);
            if ((ownCollisionRule == Team.CollisionRule.PUSH_OWN_TEAM || theirCollisionRule == Team.CollisionRule.PUSH_OWN_TEAM) && sameTeam) {
                return false;
            }
            return ownCollisionRule != Team.CollisionRule.PUSH_OTHER_TEAMS && theirCollisionRule != Team.CollisionRule.PUSH_OTHER_TEAMS || sameTeam;
        });
    }

    public static Predicate<Entity> notRiding(Entity entity) {
        return input -> {
            while (input.isPassenger()) {
                if ((input = input.getVehicle()) != entity) continue;
                return false;
            }
            return true;
        };
    }
}

