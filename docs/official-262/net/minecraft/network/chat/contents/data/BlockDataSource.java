/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat.contents.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.data.DataSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.CompilableString;
import net.minecraft.world.level.block.entity.BlockEntity;

public record BlockDataSource(CompilableString<Coordinates> coordinates) implements DataSource
{
    public static final Codec<CompilableString<Coordinates>> BLOCK_POS_CODEC = CompilableString.codec(new CompilableString.CommandParserHelper<Coordinates>(){

        @Override
        protected Coordinates parse(StringReader reader) throws CommandSyntaxException {
            return BlockPosArgument.blockPos().parse(reader);
        }

        @Override
        protected String errorMessage(String original, CommandSyntaxException exception) {
            return "Invalid coordinates path: " + original + ": " + exception.getMessage();
        }
    });
    public static final MapCodec<BlockDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BLOCK_POS_CODEC.fieldOf("block").forGetter(BlockDataSource::coordinates)).apply((Applicative)i, BlockDataSource::new));

    @Override
    public Stream<CompoundTag> getData(CommandSourceStack sender) {
        BlockEntity entity;
        BlockPos pos;
        ServerLevel level = sender.getLevel();
        if (level.isLoaded(pos = this.coordinates.compiled().getBlockPos(sender)) && (entity = level.getBlockEntity(pos)) != null) {
            return Stream.of(entity.saveWithFullMetadata(sender.registryAccess()));
        }
        return Stream.empty();
    }

    public MapCodec<BlockDataSource> codec() {
        return MAP_CODEC;
    }
}

