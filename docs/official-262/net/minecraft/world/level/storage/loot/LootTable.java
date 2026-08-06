/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectListIterator
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTable
implements Validatable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<ResourceKey<LootTable>> KEY_CODEC = ResourceKey.codec(Registries.LOOT_TABLE);
    public static final ContextKeySet DEFAULT_PARAM_SET = LootContextParamSets.ALL_PARAMS;
    public static final long RANDOMIZE_SEED = 0L;
    public static final Codec<LootTable> DIRECT_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group((App)LootContextParamSets.CODEC.lenientOptionalFieldOf("type", (Object)DEFAULT_PARAM_SET).forGetter(t -> t.paramSet), (App)Identifier.CODEC.optionalFieldOf("random_sequence").forGetter(t -> t.randomSequence), (App)LootPool.CODEC.listOf().optionalFieldOf("pools", List.of()).forGetter(t -> t.pools), (App)LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(t -> t.functions)).apply((Applicative)i, LootTable::new)));
    public static final Codec<Holder<LootTable>> CODEC = RegistryFileCodec.create(Registries.LOOT_TABLE, DIRECT_CODEC);
    public static final LootTable EMPTY = new LootTable(LootContextParamSets.EMPTY, Optional.empty(), List.of(), List.of());
    private final ContextKeySet paramSet;
    private final Optional<Identifier> randomSequence;
    private final List<LootPool> pools;
    private final List<LootItemFunction> functions;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

    private LootTable(ContextKeySet paramSet, Optional<Identifier> randomSequence, List<LootPool> pools, List<LootItemFunction> functions) {
        this.paramSet = paramSet;
        this.randomSequence = randomSequence;
        this.pools = pools;
        this.functions = functions;
        this.compositeFunction = LootItemFunctions.compose(functions);
    }

    public static Consumer<ItemStack> createStackSplitter(ServerLevel level, Consumer<ItemStack> output) {
        return result -> {
            if (!result.isItemEnabled(level.enabledFeatures())) {
                return;
            }
            if (result.getCount() < result.getMaxStackSize()) {
                output.accept((ItemStack)result);
            } else {
                ItemStack copy;
                for (int count = result.getCount(); count > 0; count -= copy.getCount()) {
                    copy = result.copyWithCount(Math.min(result.getMaxStackSize(), count));
                    output.accept(copy);
                }
            }
        };
    }

    public void getRandomItemsRaw(LootParams params, Consumer<ItemStack> output) {
        this.getRandomItemsRaw(new LootContext.Builder(params).create(this.randomSequence), output);
    }

    public void getRandomItemsRaw(LootContext context, Consumer<ItemStack> output) {
        LootContext.VisitedEntry<LootTable> breadcrumb = LootContext.createVisitedEntry(this);
        if (context.pushVisitedElement(breadcrumb)) {
            Consumer<ItemStack> decoratedOutput = LootItemFunction.decorate(this.compositeFunction, output, context);
            for (LootPool pool : this.pools) {
                pool.addRandomItems(decoratedOutput, context);
            }
            context.popVisitedElement(breadcrumb);
        } else {
            LOGGER.warn("Detected infinite loop in loot tables");
        }
    }

    public void getRandomItems(LootParams params, long optionalLootTableSeed, Consumer<ItemStack> output) {
        this.getRandomItemsRaw(new LootContext.Builder(params).withOptionalRandomSeed(optionalLootTableSeed).create(this.randomSequence), LootTable.createStackSplitter(params.getLevel(), output));
    }

    public void getRandomItems(LootParams params, Consumer<ItemStack> output) {
        this.getRandomItemsRaw(params, LootTable.createStackSplitter(params.getLevel(), output));
    }

    public void getRandomItems(LootContext context, Consumer<ItemStack> output) {
        this.getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), output));
    }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams params, RandomSource randomSource) {
        return this.getRandomItems(new LootContext.Builder(params).withOptionalRandomSource(randomSource).create(this.randomSequence));
    }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams params, long optionalLootTableSeed) {
        return this.getRandomItems(new LootContext.Builder(params).withOptionalRandomSeed(optionalLootTableSeed).create(this.randomSequence));
    }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams params) {
        return this.getRandomItems(new LootContext.Builder(params).create(this.randomSequence));
    }

    private ObjectArrayList<ItemStack> getRandomItems(LootContext context) {
        ObjectArrayList result = new ObjectArrayList();
        this.getRandomItems(context, arg_0 -> ((ObjectArrayList)result).add(arg_0));
        return result;
    }

    public ContextKeySet getParamSet() {
        return this.paramSet;
    }

    @Override
    public void validate(ValidationContext context) {
        Validatable.validate(context, "pools", this.pools);
        Validatable.validate(context, "functions", this.functions);
    }

    public void fill(Container container, LootParams params, long optionalRandomSeed) {
        LootContext context = new LootContext.Builder(params).withOptionalRandomSeed(optionalRandomSeed).create(this.randomSequence);
        ObjectArrayList<ItemStack> itemStacks = this.getRandomItems(context);
        RandomSource random = context.getRandom();
        List<Integer> availableSlots = this.getAvailableSlots(container, random);
        this.shuffleAndSplitItems(itemStacks, availableSlots.size(), random);
        for (ItemStack itemStack : itemStacks) {
            if (availableSlots.isEmpty()) {
                LOGGER.warn("Tried to over-fill a container");
                return;
            }
            if (itemStack.isEmpty()) {
                container.setItem(availableSlots.remove(availableSlots.size() - 1), ItemStack.EMPTY);
                continue;
            }
            container.setItem(availableSlots.remove(availableSlots.size() - 1), itemStack);
        }
    }

    private void shuffleAndSplitItems(ObjectArrayList<ItemStack> result, int availableSlots, RandomSource random) {
        ArrayList splittableItems = Lists.newArrayList();
        ObjectListIterator iterator = result.iterator();
        while (iterator.hasNext()) {
            ItemStack itemStack = (ItemStack)iterator.next();
            if (itemStack.isEmpty()) {
                iterator.remove();
                continue;
            }
            if (itemStack.getCount() <= 1) continue;
            splittableItems.add(itemStack);
            iterator.remove();
        }
        while (availableSlots - result.size() - splittableItems.size() > 0 && !splittableItems.isEmpty()) {
            ItemStack itemStack = (ItemStack)splittableItems.remove(Mth.nextInt(random, 0, splittableItems.size() - 1));
            int remove = Mth.nextInt(random, 1, itemStack.getCount() / 2);
            ItemStack copy = itemStack.split(remove);
            if (itemStack.getCount() > 1 && random.nextBoolean()) {
                splittableItems.add(itemStack);
            } else {
                result.add((Object)itemStack);
            }
            if (copy.getCount() > 1 && random.nextBoolean()) {
                splittableItems.add(copy);
                continue;
            }
            result.add((Object)copy);
        }
        result.addAll((Collection)splittableItems);
        Util.shuffle(result, random);
    }

    private List<Integer> getAvailableSlots(Container container, RandomSource random) {
        ObjectArrayList slots = new ObjectArrayList();
        for (int i = 0; i < container.getContainerSize(); ++i) {
            if (!container.getItem(i).isEmpty()) continue;
            slots.add((Object)i);
        }
        Util.shuffle(slots, random);
        return slots;
    }

    public static Builder lootTable() {
        return new Builder();
    }

    public static class Builder
    implements FunctionUserBuilder<Builder> {
        private final ImmutableList.Builder<LootPool> pools = ImmutableList.builder();
        private final ImmutableList.Builder<LootItemFunction> functions = ImmutableList.builder();
        private ContextKeySet paramSet = DEFAULT_PARAM_SET;
        private Optional<Identifier> randomSequence = Optional.empty();

        public Builder withPool(LootPool.Builder pool) {
            this.pools.add((Object)pool.build());
            return this;
        }

        public Builder setParamSet(ContextKeySet paramSet) {
            this.paramSet = paramSet;
            return this;
        }

        public Builder setRandomSequence(Identifier key) {
            this.randomSequence = Optional.of(key);
            return this;
        }

        @Override
        public Builder apply(LootItemFunction.Builder function) {
            this.functions.add((Object)function.build());
            return this;
        }

        @Override
        public Builder unwrap() {
            return this;
        }

        public LootTable build() {
            return new LootTable(this.paramSet, this.randomSequence, (List<LootPool>)this.pools.build(), (List<LootItemFunction>)this.functions.build());
        }
    }
}

