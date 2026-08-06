/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;

public abstract class Team {
    public boolean isAlliedTo(@Nullable Team other) {
        if (other == null) {
            return false;
        }
        return this == other;
    }

    public abstract String getName();

    public abstract MutableComponent getFormattedName(Component var1);

    public abstract boolean canSeeFriendlyInvisibles();

    public abstract boolean isAllowFriendlyFire();

    public abstract Visibility getNameTagVisibility();

    public abstract Optional<TeamColor> getColor();

    public abstract Collection<String> getPlayers();

    public abstract Visibility getDeathMessageVisibility();

    public abstract CollisionRule getCollisionRule();

    public static enum CollisionRule implements StringRepresentable
    {
        ALWAYS("always", 0),
        NEVER("never", 1),
        PUSH_OTHER_TEAMS("pushOtherTeams", 2),
        PUSH_OWN_TEAM("pushOwnTeam", 3);

        public static final Codec<CollisionRule> CODEC;
        private static final IntFunction<CollisionRule> BY_ID;
        public static final StreamCodec<ByteBuf, CollisionRule> STREAM_CODEC;
        public final String name;
        public final int id;

        private CollisionRule(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public Component getDisplayName() {
            return Component.translatable("team.collision." + this.name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(CollisionRule::values);
            BY_ID = ByIdMap.continuous(r -> r.id, CollisionRule.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, r -> r.id);
        }
    }

    public static enum Visibility implements StringRepresentable
    {
        ALWAYS("always", 0),
        NEVER("never", 1),
        HIDE_FOR_OTHER_TEAMS("hideForOtherTeams", 2),
        HIDE_FOR_OWN_TEAM("hideForOwnTeam", 3);

        public static final Codec<Visibility> CODEC;
        private static final IntFunction<Visibility> BY_ID;
        public static final StreamCodec<ByteBuf, Visibility> STREAM_CODEC;
        public final String name;
        public final int id;

        private Visibility(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public Component getDisplayName() {
            return Component.translatable("team.visibility." + this.name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Visibility::values);
            BY_ID = ByIdMap.continuous(v -> v.id, Visibility.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, v -> v.id);
        }
    }
}

