/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;

@Environment(value=EnvType.CLIENT)
public enum PrioritizeChunkUpdates {
    NONE(0, "options.prioritizeChunkUpdates.none"),
    PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
    NEARBY(2, "options.prioritizeChunkUpdates.nearby");

    private static final IntFunction<PrioritizeChunkUpdates> BY_ID;
    public static final Codec<PrioritizeChunkUpdates> LEGACY_CODEC;
    private final int id;
    private final Component caption;

    private PrioritizeChunkUpdates(int id, String key) {
        this.id = id;
        this.caption = Component.translatable(key);
    }

    public Component caption() {
        return this.caption;
    }

    static {
        BY_ID = ByIdMap.continuous(p -> p.id, PrioritizeChunkUpdates.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p -> p.id);
    }
}

