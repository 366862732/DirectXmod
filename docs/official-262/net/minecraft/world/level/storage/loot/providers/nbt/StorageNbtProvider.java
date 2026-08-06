/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;

public record StorageNbtProvider(Identifier id) implements NbtProvider
{
    public static final MapCodec<StorageNbtProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("source").forGetter(StorageNbtProvider::id)).apply((Applicative)i, StorageNbtProvider::new));

    public MapCodec<StorageNbtProvider> codec() {
        return MAP_CODEC;
    }

    @Override
    public Tag get(LootContext context) {
        return context.getLevel().getServer().getCommandStorage().get(this.id);
    }
}

