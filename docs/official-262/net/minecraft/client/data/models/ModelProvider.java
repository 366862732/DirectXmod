/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@Environment(value=EnvType.CLIENT)
public class ModelProvider
implements DataProvider {
    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider itemInfoPathProvider;
    private final PackOutput.PathProvider modelPathProvider;

    public ModelProvider(PackOutput output) {
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.itemInfoPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        ItemInfoCollector itemModels = new ItemInfoCollector();
        BlockStateGeneratorCollector blockStateGenerators = new BlockStateGeneratorCollector();
        SimpleModelCollector simpleModels = new SimpleModelCollector();
        new BlockModelGenerators(blockStateGenerators, itemModels, simpleModels).run();
        new ItemModelGenerators(itemModels, simpleModels).run();
        blockStateGenerators.validate();
        itemModels.finalizeAndValidate();
        return CompletableFuture.allOf(blockStateGenerators.save(cache, this.blockStatePathProvider), simpleModels.save(cache, this.modelPathProvider), itemModels.save(cache, this.itemInfoPathProvider));
    }

    @Override
    public final String getName() {
        return "Model Definitions";
    }

    @Environment(value=EnvType.CLIENT)
    private static class ItemInfoCollector
    implements ItemModelOutput {
        private final Map<Item, ClientItem> itemInfos = new HashMap<Item, ClientItem>();
        private final Map<Item, Item> copies = new HashMap<Item, Item>();

        private ItemInfoCollector() {
        }

        @Override
        public void accept(Item item, ItemModel.Unbaked model, ClientItem.Properties properties) {
            this.register(item, new ClientItem(model, properties));
        }

        private void register(Item item, ClientItem itemInfo) {
            ClientItem prev = this.itemInfos.put(item, itemInfo);
            if (prev != null) {
                throw new IllegalStateException("Duplicate item model definition for " + String.valueOf(item));
            }
        }

        @Override
        public void copy(Item donor, Item acceptor) {
            this.copies.put(acceptor, donor);
        }

        public void finalizeAndValidate() {
            BuiltInRegistries.ITEM.forEach(item -> {
                BlockItem blockItem;
                if (this.copies.containsKey(item)) {
                    return;
                }
                if (item instanceof BlockItem && !this.itemInfos.containsKey(blockItem = (BlockItem)item)) {
                    Identifier targetModel = ModelLocationUtils.getModelLocation(blockItem.getBlock());
                    this.accept(blockItem, ItemModelUtils.plainModel(targetModel));
                }
            });
            this.copies.forEach((acceptor, donor) -> {
                ClientItem donorInfo = this.itemInfos.get(donor);
                if (donorInfo == null) {
                    throw new IllegalStateException("Missing donor: " + String.valueOf(donor) + " -> " + String.valueOf(acceptor));
                }
                this.register((Item)acceptor, donorInfo);
            });
            List<Identifier> missingDefinitions = BuiltInRegistries.ITEM.listElements().filter(e -> !this.itemInfos.containsKey(e.value())).map(e -> e.key().identifier()).toList();
            if (!missingDefinitions.isEmpty()) {
                throw new IllegalStateException("Missing item model definitions for: " + String.valueOf(missingDefinitions));
            }
        }

        public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
            return DataProvider.saveAll(cache, ClientItem.CODEC, item -> pathProvider.json(item.builtInRegistryHolder().key().identifier()), this.itemInfos);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class BlockStateGeneratorCollector
    implements Consumer<BlockModelDefinitionGenerator> {
        private final Map<Block, BlockModelDefinitionGenerator> generators = new HashMap<Block, BlockModelDefinitionGenerator>();

        private BlockStateGeneratorCollector() {
        }

        @Override
        public void accept(BlockModelDefinitionGenerator generator) {
            Block block = generator.block();
            BlockModelDefinitionGenerator prev = this.generators.put(block, generator);
            if (prev != null) {
                throw new IllegalStateException("Duplicate blockstate definition for " + String.valueOf(block));
            }
        }

        public void validate() {
            List<Identifier> missingDefinitions = BuiltInRegistries.BLOCK.listElements().filter(e -> !this.generators.containsKey(e.value())).map(e -> e.key().identifier()).toList();
            if (!missingDefinitions.isEmpty()) {
                throw new IllegalStateException("Missing blockstate definitions for: " + String.valueOf(missingDefinitions));
            }
        }

        public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
            Map definitions = Maps.transformValues(this.generators, BlockModelDefinitionGenerator::create);
            Function<Block, Path> pathGetter = block -> pathProvider.json(block.builtInRegistryHolder().key().identifier());
            return DataProvider.saveAll(cache, BlockStateModelDispatcher.CODEC, pathGetter, definitions);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class SimpleModelCollector
    implements BiConsumer<Identifier, ModelInstance> {
        private final Map<Identifier, ModelInstance> models = new HashMap<Identifier, ModelInstance>();

        private SimpleModelCollector() {
        }

        @Override
        public void accept(Identifier id, ModelInstance contents) {
            Supplier prev = this.models.put(id, contents);
            if (prev != null) {
                throw new IllegalStateException("Duplicate model definition for " + String.valueOf(id));
            }
        }

        public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
            return DataProvider.saveAll(cache, Supplier::get, pathProvider::json, this.models);
        }
    }
}

