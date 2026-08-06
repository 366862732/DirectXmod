/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  org.apache.commons.lang3.function.TriFunction
 */
package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.data.BlockFamily;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.function.TriFunction;

public record WeatheringCopperCollection<T>(ByState<T> weathering, ByState<T> waxed) {
    public static final ByState<WeatheringCopper.WeatherState> STATES = new ByState<WeatheringCopper.WeatherState>(WeatheringCopper.WeatherState.UNAFFECTED, WeatheringCopper.WeatherState.EXPOSED, WeatheringCopper.WeatherState.WEATHERED, WeatheringCopper.WeatherState.OXIDIZED);
    public static final WeatheringCopperCollection<String> PREFIXES = new WeatheringCopperCollection<String>(new ByState<String>("", "exposed_", "weathered_", "oxidized_"), new ByState<String>("waxed_", "waxed_exposed_", "waxed_weathered_", "waxed_oxidized_"));

    public static WeatheringCopperCollection<String> prefixWithState(WeatheringCopperCollection<String> ids) {
        return WeatheringCopperCollection.zipMap(PREFIXES, ids, (T state, U id) -> state + id);
    }

    public static WeatheringCopperCollection<String> create(String name) {
        return WeatheringCopperCollection.same(ByState.create(name));
    }

    public static WeatheringCopperCollection<String> same(ByState<String> byState) {
        return new WeatheringCopperCollection<String>(byState, byState);
    }

    public static <WaxedBlock extends Block, WeatheringBlock extends Block, Id> WeatheringCopperCollection<Block> registerBlocks(WeatheringCopperCollection<Id> ids, TriFunction<Id, Function<BlockBehaviour.Properties, Block>, BlockBehaviour.Properties, Block> register, BiFunction<WeatheringCopper.WeatherState, BlockBehaviour.Properties, WaxedBlock> waxedBlockFactory, BiFunction<WeatheringCopper.WeatherState, BlockBehaviour.Properties, WeatheringBlock> weatheringFactory, Function<WeatheringCopper.WeatherState, BlockBehaviour.Properties> propertiesSupplier) {
        return ids.apply(weatheringIds -> WeatheringCopperCollection.zipMap(STATES, weatheringIds, (T state, U id) -> (Block)register.apply(id, p -> (Block)weatheringFactory.apply((WeatheringCopper.WeatherState)state, (BlockBehaviour.Properties)p), (Object)((BlockBehaviour.Properties)propertiesSupplier.apply((WeatheringCopper.WeatherState)state)))), waxedIds -> WeatheringCopperCollection.zipMap(STATES, waxedIds, (T state, U id) -> (Block)register.apply(id, p -> (Block)waxedBlockFactory.apply((WeatheringCopper.WeatherState)state, (BlockBehaviour.Properties)p), (Object)((BlockBehaviour.Properties)propertiesSupplier.apply((WeatheringCopper.WeatherState)state)))));
    }

    public static <Id> WeatheringCopperCollection<Item> registerItems(WeatheringCopperCollection<Id> ids, WeatheringCopperCollection<Block> blocks, BiFunction<Id, Block, Item> itemFactory) {
        return WeatheringCopperCollection.zipMap(ids, blocks, itemFactory);
    }

    public static WeatheringCopperCollection<BlockFamily> createFamily(BiFunction<String, WeatheringCopper.WeatherState, BlockFamily> waxedProvider, BiFunction<String, WeatheringCopper.WeatherState, BlockFamily> weatheringProvider) {
        return PREFIXES.apply(weatheringPrefixes -> WeatheringCopperCollection.zipMap(weatheringPrefixes, STATES, weatheringProvider), waxedPrefixes -> WeatheringCopperCollection.zipMap(waxedPrefixes, STATES, waxedProvider));
    }

    public List<T> asList() {
        ImmutableList.Builder builder = ImmutableList.builderWithExpectedSize((int)8);
        this.forEach(arg_0 -> ((ImmutableList.Builder)builder).add(arg_0));
        return builder.build();
    }

    public void forEach(Consumer<T> consumer) {
        this.weathering.forEach(consumer);
        this.waxed.forEach(consumer);
    }

    public <U> WeatheringCopperCollection<U> map(Function<T, U> mapper) {
        return new WeatheringCopperCollection<U>(this.weathering.map(mapper), this.waxed.map(mapper));
    }

    public <U> WeatheringCopperCollection<U> apply(Function<ByState<T>, ByState<U>> mapper) {
        return this.apply(mapper, mapper);
    }

    public <U> WeatheringCopperCollection<U> apply(Function<ByState<T>, ByState<U>> weatheringMapper, Function<ByState<T>, ByState<U>> waxedMapper) {
        return new WeatheringCopperCollection<U>(weatheringMapper.apply(this.weathering), waxedMapper.apply(this.waxed));
    }

    public static <T, U> void zipApply(WeatheringCopperCollection<T> first, WeatheringCopperCollection<U> second, BiConsumer<T, U> consumer) {
        WeatheringCopperCollection.zipApply(first.weathering, second.weathering, consumer);
        WeatheringCopperCollection.zipApply(first.waxed, second.waxed, consumer);
    }

    public static <T, U, R> WeatheringCopperCollection<R> zipMap(WeatheringCopperCollection<T> first, WeatheringCopperCollection<U> second, BiFunction<T, U, R> operation) {
        return new WeatheringCopperCollection<R>(WeatheringCopperCollection.zipMap(first.weathering, second.weathering, operation), WeatheringCopperCollection.zipMap(first.waxed, second.waxed, operation));
    }

    public void zipUnwaxedWaxed(BiConsumer<T, T> consumer) {
        WeatheringCopperCollection.zipApply(this.weathering, this.waxed, consumer);
    }

    public static <T, U> void zipApply(ByState<T> first, ByState<U> second, BiConsumer<T, U> consumer) {
        consumer.accept(first.unaffected, second.unaffected);
        consumer.accept(first.exposed, second.exposed);
        consumer.accept(first.weathered, second.weathered);
        consumer.accept(first.oxidized, second.oxidized);
    }

    public static <T, U, R> ByState<R> zipMap(ByState<T> first, ByState<U> second, BiFunction<T, U, R> operation) {
        return new ByState<R>(operation.apply(first.unaffected, second.unaffected), operation.apply(first.exposed, second.exposed), operation.apply(first.weathered, second.weathered), operation.apply(first.oxidized, second.oxidized));
    }

    public record ByState<T>(T unaffected, T exposed, T weathered, T oxidized) {
        public static <T> ByState<T> create(T value) {
            return new ByState<T>(value, value, value, value);
        }

        public <U> ByState<U> map(Function<T, U> mapper) {
            return new ByState<U>(mapper.apply(this.unaffected), mapper.apply(this.exposed), mapper.apply(this.weathered), mapper.apply(this.oxidized));
        }

        public T pick(WeatheringCopper.WeatherState state) {
            return switch (state) {
                default -> throw new MatchException(null, null);
                case WeatheringCopper.WeatherState.UNAFFECTED -> this.unaffected;
                case WeatheringCopper.WeatherState.EXPOSED -> this.exposed;
                case WeatheringCopper.WeatherState.WEATHERED -> this.weathered;
                case WeatheringCopper.WeatherState.OXIDIZED -> this.oxidized;
            };
        }

        public void forEach(Consumer<T> consumer) {
            consumer.accept(this.unaffected);
            consumer.accept(this.exposed);
            consumer.accept(this.weathered);
            consumer.accept(this.oxidized);
        }

        public void progressMapping(BiConsumer<T, T> consumer) {
            consumer.accept(this.unaffected, this.exposed);
            consumer.accept(this.exposed, this.weathered);
            consumer.accept(this.weathered, this.oxidized);
        }
    }
}

