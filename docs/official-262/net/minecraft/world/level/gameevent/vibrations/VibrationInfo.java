/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record VibrationInfo(Holder<GameEvent> gameEvent, float distance, Vec3 pos, @Nullable UUID uuid, @Nullable UUID projectileOwnerUuid, @Nullable Entity entity) {
    public static final Codec<VibrationInfo> CODEC = RecordCodecBuilder.create(i -> i.group((App)GameEvent.CODEC.fieldOf("game_event").forGetter(VibrationInfo::gameEvent), (App)Codec.floatRange((float)0.0f, (float)Float.MAX_VALUE).fieldOf("distance").forGetter(VibrationInfo::distance), (App)Vec3.CODEC.fieldOf("pos").forGetter(VibrationInfo::pos), (App)UUIDUtil.CODEC.lenientOptionalFieldOf("source").forGetter(o -> Optional.ofNullable(o.uuid())), (App)UUIDUtil.CODEC.lenientOptionalFieldOf("projectile_owner").forGetter(o -> Optional.ofNullable(o.projectileOwnerUuid()))).apply((Applicative)i, (event, distance, pos, source, projectileOwner) -> new VibrationInfo((Holder<GameEvent>)event, distance.floatValue(), (Vec3)pos, source.orElse(null), projectileOwner.orElse(null))));

    public VibrationInfo(Holder<GameEvent> gameEvent, float distance, Vec3 pos, @Nullable UUID uuid, @Nullable UUID projectileOwnerUuid) {
        this(gameEvent, distance, pos, uuid, projectileOwnerUuid, null);
    }

    public VibrationInfo(Holder<GameEvent> gameEvent, float distance, Vec3 pos, @Nullable Entity entity) {
        this(gameEvent, distance, pos, entity == null ? null : entity.getUUID(), VibrationInfo.getProjectileOwner(entity), entity);
    }

    private static @Nullable UUID getProjectileOwner(@Nullable Entity entity) {
        Projectile projectile;
        if (entity instanceof Projectile && (projectile = (Projectile)entity).getOwner() != null) {
            return projectile.getOwner().getUUID();
        }
        return null;
    }

    public Optional<Entity> getEntity(ServerLevel level) {
        return Optional.ofNullable(this.entity).or(() -> Optional.ofNullable(this.uuid).map(level::getEntity));
    }

    public Optional<Entity> getProjectileOwner(ServerLevel level) {
        return this.getEntity(level).filter(e -> e instanceof Projectile).map(e -> (Projectile)e).map(Projectile::getOwner).or(() -> Optional.ofNullable(this.projectileOwnerUuid).map(level::getEntity));
    }
}

