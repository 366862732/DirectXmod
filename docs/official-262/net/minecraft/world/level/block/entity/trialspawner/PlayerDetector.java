/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public interface PlayerDetector {
    public static final PlayerDetector NO_CREATIVE_PLAYERS = (level, selector, pos, requiredPlayerRange, requireLineOfSight) -> selector.getPlayers(level, p -> p.blockPosition().closerThan(pos, requiredPlayerRange) && !p.isCreative() && !p.isSpectator()).stream().filter(player -> !requireLineOfSight || PlayerDetector.inLineOfSight(level, Vec3.atCenterOf(pos), player.getEyePosition())).map(Entity::getUUID).toList();
    public static final PlayerDetector INCLUDING_CREATIVE_PLAYERS = (level, selector, pos, requiredPlayerRange, requireLineOfSight) -> selector.getPlayers(level, p -> p.blockPosition().closerThan(pos, requiredPlayerRange) && !p.isSpectator()).stream().filter(player -> !requireLineOfSight || PlayerDetector.inLineOfSight(level, Vec3.atCenterOf(pos), player.getEyePosition())).map(Entity::getUUID).toList();
    public static final PlayerDetector SHEEP = (level, selector, pos, requiredPlayerRange, requireLineOfSight) -> {
        AABB area = new AABB(pos).inflate(requiredPlayerRange);
        return selector.getEntities(level, EntityTypes.SHEEP, area, LivingEntity::isAlive).stream().filter(entity -> !requireLineOfSight || PlayerDetector.inLineOfSight(level, Vec3.atCenterOf(pos), entity.getEyePosition())).map(Entity::getUUID).toList();
    };

    public List<UUID> detect(ServerLevel var1, EntitySelector var2, BlockPos var3, double var4, boolean var6);

    private static boolean inLineOfSight(Level level, Vec3 origin, Vec3 dest) {
        BlockHitResult hitResult = level.clip(new ClipContext(dest, origin, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hitResult.getBlockPos().equals(BlockPos.containing(origin)) || hitResult.getType() == HitResult.Type.MISS;
    }

    public static interface EntitySelector {
        public static final EntitySelector SELECT_FROM_LEVEL = new EntitySelector(){

            public List<ServerPlayer> getPlayers(ServerLevel level, Predicate<? super Player> selector) {
                return level.getPlayers(selector);
            }

            @Override
            public <T extends Entity> List<T> getEntities(ServerLevel level, EntityTypeTest<Entity, T> type, AABB aabb, Predicate<? super T> selector) {
                return level.getEntities(type, aabb, selector);
            }
        };

        public List<? extends Player> getPlayers(ServerLevel var1, Predicate<? super Player> var2);

        public <T extends Entity> List<T> getEntities(ServerLevel var1, EntityTypeTest<Entity, T> var2, AABB var3, Predicate<? super T> var4);

        public static EntitySelector onlySelectPlayer(Player player) {
            return EntitySelector.onlySelectPlayers(List.of(player));
        }

        public static EntitySelector onlySelectPlayers(final List<Player> players) {
            return new EntitySelector(){

                public List<Player> getPlayers(ServerLevel level, Predicate<? super Player> selector) {
                    return players.stream().filter(selector).toList();
                }

                @Override
                public <T extends Entity> List<T> getEntities(ServerLevel level, EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector) {
                    return players.stream().map(type::tryCast).filter(Objects::nonNull).filter(selector).toList();
                }
            };
        }
    }
}

