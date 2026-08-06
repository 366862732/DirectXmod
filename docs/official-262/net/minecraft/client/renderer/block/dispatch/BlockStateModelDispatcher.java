/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.block.dispatch;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.VariantSelector;
import net.minecraft.client.renderer.block.dispatch.multipart.MultiPartModel;
import net.minecraft.client.renderer.block.dispatch.multipart.Selector;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record BlockStateModelDispatcher(Optional<SimpleModelSelectors> simpleModels, Optional<MultiPartDefinition> multiPart) {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<BlockStateModelDispatcher> CODEC = RecordCodecBuilder.create(i -> i.group((App)SimpleModelSelectors.CODEC.optionalFieldOf("variants").forGetter(BlockStateModelDispatcher::simpleModels), (App)MultiPartDefinition.CODEC.optionalFieldOf("multipart").forGetter(BlockStateModelDispatcher::multiPart)).apply((Applicative)i, BlockStateModelDispatcher::new)).validate(o -> {
        if (o.simpleModels().isEmpty() && o.multiPart().isEmpty()) {
            return DataResult.error(() -> "Neither 'variants' nor 'multipart' found");
        }
        return DataResult.success((Object)o);
    });

    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(StateDefinition<Block, BlockState> stateDefinition, Supplier<String> source) {
        IdentityHashMap<BlockState, BlockStateModel.UnbakedRoot> matchedStates = new IdentityHashMap<BlockState, BlockStateModel.UnbakedRoot>();
        this.simpleModels.ifPresent(s -> s.instantiate(stateDefinition, source, (state, model) -> {
            BlockStateModel.UnbakedRoot previousValue = matchedStates.put((BlockState)state, (BlockStateModel.UnbakedRoot)model);
            if (previousValue != null) {
                throw new IllegalArgumentException("Overlapping definition on state: " + String.valueOf(state));
            }
        }));
        this.multiPart.ifPresent(m -> {
            ImmutableList possibleStates = stateDefinition.getPossibleStates();
            MultiPartModel.Unbaked model = m.instantiate(stateDefinition);
            for (BlockState state : possibleStates) {
                matchedStates.putIfAbsent(state, model);
            }
        });
        return matchedStates;
    }

    @Environment(value=EnvType.CLIENT)
    public record MultiPartDefinition(List<Selector> selectors) {
        public static final Codec<MultiPartDefinition> CODEC = ExtraCodecs.nonEmptyList(Selector.CODEC.listOf()).xmap(MultiPartDefinition::new, MultiPartDefinition::selectors);

        public MultiPartModel.Unbaked instantiate(StateDefinition<Block, BlockState> stateDefinition) {
            ImmutableList.Builder instantiatedSelectors = ImmutableList.builderWithExpectedSize((int)this.selectors.size());
            for (Selector selector : this.selectors) {
                instantiatedSelectors.add(new MultiPartModel.Selector<BlockStateModel.Unbaked>(selector.instantiate(stateDefinition), selector.variant()));
            }
            return new MultiPartModel.Unbaked((List<MultiPartModel.Selector<BlockStateModel.Unbaked>>)instantiatedSelectors.build());
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record SimpleModelSelectors(Map<String, BlockStateModel.Unbaked> models) {
        public static final Codec<SimpleModelSelectors> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap((Codec)Codec.STRING, BlockStateModel.Unbaked.CODEC)).xmap(SimpleModelSelectors::new, SimpleModelSelectors::models);

        public void instantiate(StateDefinition<Block, BlockState> stateDefinition, Supplier<String> source, BiConsumer<BlockState, BlockStateModel.UnbakedRoot> output) {
            this.models.forEach((selectorString, model) -> {
                try {
                    Predicate selector = VariantSelector.predicate(stateDefinition, selectorString);
                    BlockStateModel.UnbakedRoot wrapper = model.asRoot();
                    for (BlockState state : stateDefinition.getPossibleStates()) {
                        if (!selector.test(state)) continue;
                        output.accept(state, wrapper);
                    }
                }
                catch (Exception e) {
                    LOGGER.warn("Exception loading blockstate definition: '{}' for variant: '{}': {}", new Object[]{source.get(), selectorString, e.getMessage()});
                }
            });
        }
    }
}

