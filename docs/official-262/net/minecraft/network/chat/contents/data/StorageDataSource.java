/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat.contents.data;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.data.DataSource;
import net.minecraft.resources.Identifier;

public record StorageDataSource(Identifier id) implements DataSource
{
    public static final MapCodec<StorageDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("storage").forGetter(StorageDataSource::id)).apply((Applicative)i, StorageDataSource::new));

    @Override
    public Stream<CompoundTag> getData(CommandSourceStack sender) {
        CompoundTag tag = sender.getServer().getCommandStorage().get(this.id);
        return Stream.of(tag);
    }

    public MapCodec<StorageDataSource> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "storage=" + String.valueOf(this.id);
    }
}

