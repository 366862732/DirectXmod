/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.UUIDLookup;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public final class EntityReference<StoredEntityType extends UniquelyIdentifyable> {
    private static final Codec<? extends EntityReference<?>> CODEC = UUIDUtil.CODEC.xmap(EntityReference::new, EntityReference::getUUID);
    private static final StreamCodec<ByteBuf, ? extends EntityReference<?>> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(EntityReference::new, EntityReference::getUUID);
    private Either<UUID, StoredEntityType> entity;

    public static <Type extends UniquelyIdentifyable> Codec<EntityReference<Type>> codec() {
        return CODEC;
    }

    public static <Type extends UniquelyIdentifyable> StreamCodec<ByteBuf, EntityReference<Type>> streamCodec() {
        return STREAM_CODEC;
    }

    private EntityReference(StoredEntityType entity) {
        this.entity = Either.right(entity);
    }

    private EntityReference(UUID uuid) {
        this.entity = Either.left((Object)uuid);
    }

    @Contract(value="!null -> !null; null -> null")
    public static <T extends UniquelyIdentifyable> @Nullable EntityReference<T> of(@Nullable T entity) {
        return entity != null ? new EntityReference<T>(entity) : null;
    }

    public static <T extends UniquelyIdentifyable> EntityReference<T> of(UUID uuid) {
        return new EntityReference(uuid);
    }

    public UUID getUUID() {
        return (UUID)this.entity.map(uuid -> uuid, UniquelyIdentifyable::getUUID);
    }

    public @Nullable StoredEntityType getEntity(UUIDLookup<? extends UniquelyIdentifyable> lookup, Class<StoredEntityType> clazz) {
        StoredEntityType resolved;
        Optional uuid;
        Optional stored = this.entity.right();
        if (stored.isPresent()) {
            UniquelyIdentifyable storedEntity = (UniquelyIdentifyable)stored.get();
            if (storedEntity.isRemoved()) {
                this.entity = Either.left((Object)storedEntity.getUUID());
            } else {
                return (StoredEntityType)storedEntity;
            }
        }
        if ((uuid = this.entity.left()).isPresent() && (resolved = this.resolve(lookup.lookup((UUID)uuid.get()), clazz)) != null && !resolved.isRemoved()) {
            this.entity = Either.right(resolved);
            return resolved;
        }
        return null;
    }

    public @Nullable StoredEntityType getEntity(Level level, Class<StoredEntityType> clazz) {
        if (Player.class.isAssignableFrom(clazz)) {
            return this.getEntity(level::getPlayerInAnyDimension, clazz);
        }
        return this.getEntity(level::getEntityInAnyDimension, clazz);
    }

    private @Nullable StoredEntityType resolve(@Nullable UniquelyIdentifyable entity, Class<StoredEntityType> clazz) {
        if (entity != null && clazz.isAssignableFrom(entity.getClass())) {
            return (StoredEntityType)((UniquelyIdentifyable)clazz.cast(entity));
        }
        return null;
    }

    public boolean matches(StoredEntityType entity) {
        return this.getUUID().equals(entity.getUUID());
    }

    public void store(ValueOutput output, String key) {
        output.store(key, UUIDUtil.CODEC, this.getUUID());
    }

    public static void store(@Nullable EntityReference<?> reference, ValueOutput output, String key) {
        if (reference != null) {
            reference.store(output, key);
        }
    }

    public static <StoredEntityType extends UniquelyIdentifyable> @Nullable StoredEntityType get(@Nullable EntityReference<StoredEntityType> reference, Level level, Class<StoredEntityType> clazz) {
        return reference != null ? (StoredEntityType)reference.getEntity(level, clazz) : null;
    }

    public static @Nullable Entity getEntity(@Nullable EntityReference<Entity> reference, Level level) {
        return EntityReference.get(reference, level, Entity.class);
    }

    public static @Nullable LivingEntity getLivingEntity(@Nullable EntityReference<LivingEntity> reference, Level level) {
        return EntityReference.get(reference, level, LivingEntity.class);
    }

    public static @Nullable Player getPlayer(@Nullable EntityReference<Player> reference, Level level) {
        return EntityReference.get(reference, level, Player.class);
    }

    public static <StoredEntityType extends UniquelyIdentifyable> @Nullable EntityReference<StoredEntityType> read(ValueInput input, String key) {
        return input.read(key, EntityReference.codec()).orElse(null);
    }

    public static <StoredEntityType extends UniquelyIdentifyable> @Nullable EntityReference<StoredEntityType> readWithOldOwnerConversion(ValueInput input, String key, Level level) {
        Optional<UUID> uuid = input.read(key, UUIDUtil.CODEC);
        if (uuid.isPresent()) {
            return EntityReference.of(uuid.get());
        }
        return input.getString(key).map(oldName -> OldUsersConverter.convertMobOwnerIfNecessary(level.getServer(), oldName)).map(EntityReference::new).orElse(null);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EntityReference)) return false;
        EntityReference reference = (EntityReference)obj;
        if (!this.getUUID().equals(reference.getUUID())) return false;
        return true;
    }

    public int hashCode() {
        return this.getUUID().hashCode();
    }
}

