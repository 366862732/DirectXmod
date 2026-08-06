/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(value=EnvType.CLIENT)
public class MultiVariantGenerator
implements BlockModelDefinitionGenerator {
    private final Block block;
    private final List<Entry> entries;
    private final Set<Property<?>> seenProperties;

    private MultiVariantGenerator(Block block, List<Entry> entries, Set<Property<?>> seenProperties) {
        this.block = block;
        this.entries = entries;
        this.seenProperties = seenProperties;
    }

    private static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> seenProperties, Block block, PropertyDispatch<?> generator) {
        List<Property<?>> addedProperties = generator.getDefinedProperties();
        addedProperties.forEach(property -> {
            if (block.getStateDefinition().getProperty(property.getName()) != property) {
                throw new IllegalStateException("Property " + String.valueOf(property) + " is not defined for block " + String.valueOf(block));
            }
            if (seenProperties.contains(property)) {
                throw new IllegalStateException("Values of property " + String.valueOf(property) + " already defined for block " + String.valueOf(block));
            }
        });
        HashSet newSeenProperties = new HashSet(seenProperties);
        newSeenProperties.addAll(addedProperties);
        return newSeenProperties;
    }

    public MultiVariantGenerator with(PropertyDispatch<VariantMutator> newStage) {
        Set<Property<?>> newSeenProperties = MultiVariantGenerator.validateAndExpandProperties(this.seenProperties, this.block, newStage);
        List<Entry> newEntries = this.entries.stream().flatMap(entry -> entry.apply(newStage)).toList();
        return new MultiVariantGenerator(this.block, newEntries, newSeenProperties);
    }

    public MultiVariantGenerator with(VariantMutator singleMutator) {
        List<Entry> newEntries = this.entries.stream().flatMap(entry -> entry.apply(singleMutator)).toList();
        return new MultiVariantGenerator(this.block, newEntries, this.seenProperties);
    }

    @Override
    public BlockStateModelDispatcher create() {
        HashMap<String, BlockStateModel.Unbaked> variants = new HashMap<String, BlockStateModel.Unbaked>();
        for (Entry entry : this.entries) {
            variants.put(entry.properties.getKey(), entry.variant.toUnbaked());
        }
        return new BlockStateModelDispatcher(Optional.of(new BlockStateModelDispatcher.SimpleModelSelectors(variants)), Optional.empty());
    }

    @Override
    public Block block() {
        return this.block;
    }

    public static Empty dispatch(Block block) {
        return new Empty(block);
    }

    public static MultiVariantGenerator dispatch(Block block, MultiVariant initialModel) {
        return new MultiVariantGenerator(block, List.of(new Entry(PropertyValueList.EMPTY, initialModel)), Set.of());
    }

    @Environment(value=EnvType.CLIENT)
    private record Entry(PropertyValueList properties, MultiVariant variant) {
        public Stream<Entry> apply(PropertyDispatch<VariantMutator> stage) {
            return stage.getEntries().entrySet().stream().map(property -> {
                PropertyValueList newSelector = this.properties.extend((PropertyValueList)property.getKey());
                MultiVariant newVariants = this.variant.with((VariantMutator)property.getValue());
                return new Entry(newSelector, newVariants);
            });
        }

        public Stream<Entry> apply(VariantMutator mutator) {
            return Stream.of(new Entry(this.properties, this.variant.with(mutator)));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Empty {
        private final Block block;

        public Empty(Block block) {
            this.block = block;
        }

        public MultiVariantGenerator with(PropertyDispatch<MultiVariant> newStage) {
            Set<Property<?>> newSeenProperties = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, newStage);
            List<Entry> newEntries = newStage.getEntries().entrySet().stream().map(e -> new Entry((PropertyValueList)e.getKey(), (MultiVariant)e.getValue())).toList();
            return new MultiVariantGenerator(this.block, newEntries, newSeenProperties);
        }
    }
}

