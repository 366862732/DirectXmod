/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public record TicketType(long timeout, @Flags int flags) {
    public static final long NO_TIMEOUT = 0L;
    public static final int FLAG_PERSIST = 1;
    public static final int FLAG_LOADING = 2;
    public static final int FLAG_SIMULATION = 4;
    public static final int FLAG_KEEP_DIMENSION_ACTIVE = 8;
    public static final int FLAG_CAN_EXPIRE_IF_UNLOADED = 16;
    public static final TicketType PLAYER_SPAWN = TicketType.register("player_spawn", 20L, 2);
    public static final TicketType SPAWN_SEARCH = TicketType.register("spawn_search", 1L, 2);
    public static final TicketType DRAGON = TicketType.register("dragon", 0L, 6);
    public static final TicketType PLAYER_LOADING = TicketType.register("player_loading", 0L, 2);
    public static final TicketType PLAYER_SIMULATION = TicketType.register("player_simulation", 0L, 12);
    public static final TicketType FORCED = TicketType.register("forced", 0L, 15);
    public static final TicketType PORTAL = TicketType.register("portal", 300L, 15);
    public static final TicketType ENDER_PEARL = TicketType.register("ender_pearl", 40L, 14);
    public static final TicketType UNKNOWN = TicketType.register("unknown", 1L, 18);

    private static TicketType register(String name, long timeout, @Flags int flags) {
        return Registry.register(BuiltInRegistries.TICKET_TYPE, name, new TicketType(timeout, flags));
    }

    public boolean persist() {
        return (this.flags & 1) != 0;
    }

    public boolean doesLoad() {
        return (this.flags & 2) != 0;
    }

    public boolean doesSimulate() {
        return (this.flags & 4) != 0;
    }

    public boolean shouldKeepDimensionActive() {
        return (this.flags & 8) != 0;
    }

    public boolean canExpireIfUnloaded() {
        return (this.flags & 0x10) != 0;
    }

    public boolean hasTimeout() {
        return this.timeout != 0L;
    }

    @Retention(value=RetentionPolicy.CLASS)
    @Target(value={ElementType.TYPE_USE})
    public static @interface Flags {
    }
}

