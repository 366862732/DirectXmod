/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.ImmutableBiMap
 *  com.google.common.collect.ImmutableBiMap$Builder
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.level.block;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;

public interface WeatheringCopper
extends ChangeOverTimeBlock<WeatherState> {
    public static final Supplier<BiMap<Block, Block>> NEXT_BY_BLOCK = Suppliers.memoize(() -> {
        ImmutableBiMap.Builder builder = ImmutableBiMap.builder();
        Stream.of(Blocks.COPPER_BLOCK, Blocks.CUT_COPPER, Blocks.CHISELED_COPPER, Blocks.CUT_COPPER_SLAB, Blocks.CUT_COPPER_STAIRS, Blocks.COPPER_DOOR, Blocks.COPPER_TRAPDOOR, Blocks.COPPER_BARS, Blocks.COPPER_GRATE, Blocks.COPPER_BULB, Blocks.COPPER_LANTERN, Blocks.COPPER_CHEST, Blocks.COPPER_GOLEM_STATUE, Blocks.LIGHTNING_ROD, Blocks.COPPER_CHAIN).forEach(collection -> collection.weathering().progressMapping((arg_0, arg_1) -> ((ImmutableBiMap.Builder)builder).put(arg_0, arg_1)));
        return builder.build();
    });
    public static final Supplier<BiMap<Block, Block>> PREVIOUS_BY_BLOCK = Suppliers.memoize(() -> NEXT_BY_BLOCK.get().inverse());

    public static Optional<Block> getPrevious(Block block) {
        return Optional.ofNullable((Block)PREVIOUS_BY_BLOCK.get().get((Object)block));
    }

    public static Block getFirst(Block block) {
        Block candiate = block;
        Block previous = (Block)PREVIOUS_BY_BLOCK.get().get((Object)candiate);
        while (previous != null) {
            candiate = previous;
            previous = (Block)PREVIOUS_BY_BLOCK.get().get((Object)candiate);
        }
        return candiate;
    }

    public static Optional<BlockState> getPrevious(BlockState state) {
        return WeatheringCopper.getPrevious(state.getBlock()).map(s -> s.withPropertiesOf(state));
    }

    public static Optional<Block> getNext(Block block) {
        return Optional.ofNullable((Block)NEXT_BY_BLOCK.get().get((Object)block));
    }

    public static BlockState getFirst(BlockState state) {
        return WeatheringCopper.getFirst(state.getBlock()).withPropertiesOf(state);
    }

    @Override
    default public Optional<BlockState> getNext(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).map(s -> s.withPropertiesOf(state));
    }

    @Override
    default public float getChanceModifier() {
        if (this.getAge() == WeatherState.UNAFFECTED) {
            return 0.75f;
        }
        return 1.0f;
    }

    public static enum WeatherState implements StringRepresentable
    {
        UNAFFECTED("unaffected"),
        EXPOSED("exposed"),
        WEATHERED("weathered"),
        OXIDIZED("oxidized");

        public static final IntFunction<WeatherState> BY_ID;
        public static final Codec<WeatherState> CODEC;
        public static final StreamCodec<ByteBuf, WeatherState> STREAM_CODEC;
        private final String name;

        private WeatherState(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static void forEach(Consumer<WeatherState> consumer) {
            for (WeatherState weatherState : WeatherState.values()) {
                consumer.accept(weatherState);
            }
        }

        public WeatherState next() {
            return BY_ID.apply(this.ordinal() + 1);
        }

        public WeatherState previous() {
            return BY_ID.apply(this.ordinal() - 1);
        }

        static {
            BY_ID = ByIdMap.continuous(Enum::ordinal, WeatherState.values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
            CODEC = StringRepresentable.fromEnum(WeatherState::values);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
        }
    }
}

