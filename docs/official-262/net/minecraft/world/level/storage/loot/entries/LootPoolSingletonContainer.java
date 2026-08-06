/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.Products$P4
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Instance
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Mu
 */
package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class LootPoolSingletonContainer
extends LootPoolEntryContainer {
    public static final int DEFAULT_WEIGHT = 1;
    public static final int DEFAULT_QUALITY = 0;
    protected final int weight;
    protected final int quality;
    protected final List<LootItemFunction> functions;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
    private final LootPoolEntry entry = new EntryBase(this){
        final /* synthetic */ LootPoolSingletonContainer this$0;
        {
            LootPoolSingletonContainer lootPoolSingletonContainer = this$0;
            Objects.requireNonNull(lootPoolSingletonContainer);
            this.this$0 = lootPoolSingletonContainer;
            super(this$0);
        }

        @Override
        public void createItemStack(Consumer<ItemStack> output, LootContext context) {
            this.this$0.createItemStack(LootItemFunction.decorate(this.this$0.compositeFunction, output, context), context);
        }
    };

    protected LootPoolSingletonContainer(int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(conditions);
        this.weight = weight;
        this.quality = quality;
        this.functions = functions;
        this.compositeFunction = LootItemFunctions.compose(functions);
    }

    public abstract MapCodec<? extends LootPoolSingletonContainer> codec();

    protected static <T extends LootPoolSingletonContainer> Products.P4<RecordCodecBuilder.Mu<T>, Integer, Integer, List<LootItemCondition>, List<LootItemFunction>> singletonFields(RecordCodecBuilder.Instance<T> i) {
        return i.group((App)Codec.INT.optionalFieldOf("weight", (Object)1).forGetter(e -> e.weight), (App)Codec.INT.optionalFieldOf("quality", (Object)0).forGetter(e -> e.quality)).and(LootPoolSingletonContainer.commonFields(i).t1()).and((App)LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(e -> e.functions));
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "functions", this.functions);
    }

    protected abstract void createItemStack(Consumer<ItemStack> var1, LootContext var2);

    @Override
    public boolean expand(LootContext context, Consumer<LootPoolEntry> output) {
        if (this.canRun(context)) {
            output.accept(this.entry);
            return true;
        }
        return false;
    }

    public static Builder<?> simpleBuilder(EntryConstructor constructor) {
        return new DummyBuilder(constructor);
    }

    private static class DummyBuilder
    extends Builder<DummyBuilder> {
        private final EntryConstructor constructor;

        public DummyBuilder(EntryConstructor constructor) {
            this.constructor = constructor;
        }

        @Override
        protected DummyBuilder getThis() {
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return this.constructor.build(this.weight, this.quality, this.getConditions(), this.getFunctions());
        }
    }

    @FunctionalInterface
    protected static interface EntryConstructor {
        public LootPoolSingletonContainer build(int var1, int var2, List<LootItemCondition> var3, List<LootItemFunction> var4);
    }

    public static abstract class Builder<T extends Builder<T>>
    extends LootPoolEntryContainer.Builder<T>
    implements FunctionUserBuilder<T> {
        protected int weight = 1;
        protected int quality = 0;
        private final ImmutableList.Builder<LootItemFunction> functions = ImmutableList.builder();

        @Override
        public T apply(LootItemFunction.Builder function) {
            this.functions.add((Object)function.build());
            return (T)((Builder)this.getThis());
        }

        protected List<LootItemFunction> getFunctions() {
            return this.functions.build();
        }

        public T setWeight(int weight) {
            this.weight = weight;
            return (T)((Builder)this.getThis());
        }

        public T setQuality(int quality) {
            this.quality = quality;
            return (T)((Builder)this.getThis());
        }
    }

    protected abstract class EntryBase
    implements LootPoolEntry {
        final /* synthetic */ LootPoolSingletonContainer this$0;

        protected EntryBase(LootPoolSingletonContainer this$0) {
            LootPoolSingletonContainer lootPoolSingletonContainer = this$0;
            Objects.requireNonNull(lootPoolSingletonContainer);
            this.this$0 = lootPoolSingletonContainer;
        }

        @Override
        public int getWeight(float luck) {
            return Math.max(Mth.floor((float)this.this$0.weight + (float)this.this$0.quality * luck), 0);
        }
    }
}

