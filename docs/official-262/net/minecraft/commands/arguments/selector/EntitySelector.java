/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments.selector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.CompilableString;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntitySelector {
    public static final int INFINITE = Integer.MAX_VALUE;
    public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_ARBITRARY = (p, c) -> {};
    private static final EntityTypeTest<Entity, ?> ANY_TYPE = new EntityTypeTest<Entity, Entity>(){

        @Override
        public Entity tryCast(Entity entity) {
            return entity;
        }

        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    };
    public static final Codec<CompilableString<EntitySelector>> COMPILABLE_CODEC = CompilableString.codec(new CompilableString.CommandParserHelper<EntitySelector>(){

        @Override
        protected EntitySelector parse(StringReader reader) throws CommandSyntaxException {
            return new EntitySelectorParser(reader, true).parse();
        }

        @Override
        protected String errorMessage(String original, CommandSyntaxException exception) {
            return "Invalid selector component: " + original + ": " + exception.getMessage();
        }
    });
    private final int maxResults;
    private final boolean includesEntities;
    private final boolean worldLimited;
    private final List<Predicate<Entity>> contextFreePredicates;
    private final @Nullable MinMaxBounds.Doubles range;
    private final Function<Vec3, Vec3> position;
    private final @Nullable AABB aabb;
    private final BiConsumer<Vec3, List<? extends Entity>> order;
    private final boolean currentEntity;
    private final @Nullable String playerName;
    private final @Nullable UUID entityUUID;
    private final EntityTypeTest<Entity, ?> type;
    private final boolean usesSelector;

    public EntitySelector(int maxResults, boolean includesEntities, boolean worldLimited, List<Predicate<Entity>> contextFreePredicates, @Nullable MinMaxBounds.Doubles range, Function<Vec3, Vec3> position, @Nullable AABB aabb, BiConsumer<Vec3, List<? extends Entity>> order, boolean currentEntity, @Nullable String playerName, @Nullable UUID entityUUID, @Nullable EntityType<?> type, boolean usesSelector) {
        this.maxResults = maxResults;
        this.includesEntities = includesEntities;
        this.worldLimited = worldLimited;
        this.contextFreePredicates = contextFreePredicates;
        this.range = range;
        this.position = position;
        this.aabb = aabb;
        this.order = order;
        this.currentEntity = currentEntity;
        this.playerName = playerName;
        this.entityUUID = entityUUID;
        this.type = type == null ? ANY_TYPE : type;
        this.usesSelector = usesSelector;
    }

    public int getMaxResults() {
        return this.maxResults;
    }

    public boolean includesEntities() {
        return this.includesEntities;
    }

    public boolean isSelfSelector() {
        return this.currentEntity;
    }

    public boolean isWorldLimited() {
        return this.worldLimited;
    }

    public boolean usesSelector() {
        return this.usesSelector;
    }

    private void checkPermissions(CommandSourceStack sender) throws CommandSyntaxException {
        if (this.usesSelector && !sender.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)) {
            throw EntityArgument.ERROR_SELECTORS_NOT_ALLOWED.create();
        }
    }

    public Entity findSingleEntity(CommandSourceStack sender) throws CommandSyntaxException {
        this.checkPermissions(sender);
        List<? extends Entity> entities = this.findEntities(sender);
        if (entities.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        }
        if (entities.size() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
        }
        return entities.get(0);
    }

    public List<? extends Entity> findEntities(CommandSourceStack sender) throws CommandSyntaxException {
        this.checkPermissions(sender);
        if (!this.includesEntities) {
            return this.findPlayers(sender);
        }
        if (this.playerName != null) {
            ServerPlayer result = sender.getServer().getPlayerList().getPlayerByName(this.playerName);
            if (result == null) {
                return List.of();
            }
            return List.of(result);
        }
        if (this.entityUUID != null) {
            for (ServerLevel level : sender.getServer().getAllLevels()) {
                Entity entity = level.getEntity(this.entityUUID);
                if (entity == null) continue;
                if (!entity.getType().isEnabled(sender.enabledFeatures())) break;
                return List.of(entity);
            }
            return List.of();
        }
        Vec3 pos = this.position.apply(sender.getPosition());
        AABB absoluteAabb = this.getAbsoluteAabb(pos);
        if (this.currentEntity) {
            Predicate<Entity> predicate = this.getPredicate(pos, absoluteAabb, null);
            if (sender.getEntity() != null && predicate.test(sender.getEntity())) {
                return List.of(sender.getEntity());
            }
            return List.of();
        }
        Predicate<Entity> predicate = this.getPredicate(pos, absoluteAabb, sender.enabledFeatures());
        ObjectArrayList result = new ObjectArrayList();
        if (this.isWorldLimited()) {
            this.addEntities((List<Entity>)result, sender.getLevel(), absoluteAabb, predicate);
        } else {
            for (ServerLevel level : sender.getServer().getAllLevels()) {
                this.addEntities((List<Entity>)result, level, absoluteAabb, predicate);
            }
        }
        return this.sortAndLimit(pos, (List)result);
    }

    private void addEntities(List<Entity> result, ServerLevel level, @Nullable AABB absoluteAABB, Predicate<Entity> predicate) {
        int limit = this.getResultLimit();
        if (result.size() >= limit) {
            return;
        }
        if (absoluteAABB != null) {
            level.getEntities(this.type, absoluteAABB, predicate, result, limit);
        } else {
            level.getEntities(this.type, predicate, result, limit);
        }
    }

    private int getResultLimit() {
        return this.order == ORDER_ARBITRARY ? this.maxResults : Integer.MAX_VALUE;
    }

    public ServerPlayer findSinglePlayer(CommandSourceStack sender) throws CommandSyntaxException {
        this.checkPermissions(sender);
        List<ServerPlayer> players = this.findPlayers(sender);
        if (players.size() != 1) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        }
        return players.get(0);
    }

    public List<ServerPlayer> findPlayers(CommandSourceStack sender) throws CommandSyntaxException {
        Object result;
        this.checkPermissions(sender);
        if (this.playerName != null) {
            ServerPlayer result2 = sender.getServer().getPlayerList().getPlayerByName(this.playerName);
            if (result2 == null) {
                return List.of();
            }
            return List.of(result2);
        }
        if (this.entityUUID != null) {
            ServerPlayer result3 = sender.getServer().getPlayerList().getPlayer(this.entityUUID);
            if (result3 == null) {
                return List.of();
            }
            return List.of(result3);
        }
        Vec3 pos = this.position.apply(sender.getPosition());
        AABB absoluteAabb = this.getAbsoluteAabb(pos);
        Predicate<Entity> predicate = this.getPredicate(pos, absoluteAabb, null);
        if (this.currentEntity) {
            ServerPlayer player;
            Entity entity = sender.getEntity();
            if (entity instanceof ServerPlayer && predicate.test(player = (ServerPlayer)entity)) {
                return List.of(player);
            }
            return List.of();
        }
        int limit = this.getResultLimit();
        if (this.isWorldLimited()) {
            result = sender.getLevel().getPlayers(predicate, limit);
        } else {
            result = new ObjectArrayList();
            for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                if (!predicate.test(player)) continue;
                result.add(player);
                if (result.size() < limit) continue;
                return result;
            }
        }
        return this.sortAndLimit(pos, (List)result);
    }

    private @Nullable AABB getAbsoluteAabb(Vec3 pos) {
        return this.aabb != null ? this.aabb.move(pos) : null;
    }

    private Predicate<Entity> getPredicate(Vec3 pos, @Nullable AABB absoluteAabb, @Nullable FeatureFlagSet enabledFeatures) {
        ObjectArrayList completePredicates;
        boolean filterRange;
        boolean filterAabb;
        boolean filterFeatures = enabledFeatures != null;
        int extraCount = (filterFeatures ? 1 : 0) + ((filterAabb = absoluteAabb != null) ? 1 : 0) + ((filterRange = this.range != null) ? 1 : 0);
        if (extraCount == 0) {
            completePredicates = this.contextFreePredicates;
        } else {
            ObjectArrayList predicates = new ObjectArrayList(this.contextFreePredicates.size() + extraCount);
            predicates.addAll(this.contextFreePredicates);
            if (filterFeatures) {
                predicates.add(e -> e.getType().isEnabled(enabledFeatures));
            }
            if (filterAabb) {
                predicates.add(e -> absoluteAabb.intersects(e.getBoundingBox()));
            }
            if (filterRange) {
                predicates.add(e -> this.range.matchesSqr(e.distanceToSqr(pos)));
            }
            completePredicates = predicates;
        }
        return Util.allOf(completePredicates);
    }

    private <T extends Entity> List<T> sortAndLimit(Vec3 pos, List<T> result) {
        if (result.size() > 1) {
            this.order.accept(pos, result);
        }
        return result.subList(0, Math.min(this.maxResults, result.size()));
    }

    public static Component joinNames(List<? extends Entity> entities) {
        return ComponentUtils.formatList(entities, Entity::getDisplayName);
    }
}

