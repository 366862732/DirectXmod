/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.entries;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class NestedLootTable
extends LootPoolSingletonContainer {
    public static final MapCodec<NestedLootTable> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.either(LootTable.KEY_CODEC, LootTable.DIRECT_CODEC).fieldOf("value").forGetter(e -> e.contents)).and(NestedLootTable.singletonFields(i)).apply((Applicative)i, NestedLootTable::new));
    public static final ProblemReporter.PathElement INLINE_LOOT_TABLE_PATH_ELEMENT = new ProblemReporter.PathElement(){

        @Override
        public String get() {
            return "->{inline}";
        }
    };
    private final Either<ResourceKey<LootTable>, LootTable> contents;

    private NestedLootTable(Either<ResourceKey<LootTable>, LootTable> contents, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.contents = contents;
    }

    public MapCodec<NestedLootTable> codec() {
        return MAP_CODEC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> output, LootContext context) {
        ((LootTable)this.contents.map(name -> context.getResolver().get(name).map(Holder::value).orElse(LootTable.EMPTY), table -> table)).getRandomItemsRaw(context, output);
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        this.contents.ifLeft(id -> Validatable.validateReference(context, id)).ifRight(lootTable -> lootTable.validate(context.forChild(INLINE_LOOT_TABLE_PATH_ELEMENT)));
    }

    public static LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceKey<LootTable> name) {
        return NestedLootTable.simpleBuilder((weight, quality, conditions, functions) -> new NestedLootTable((Either<ResourceKey<LootTable>, LootTable>)Either.left((Object)name), weight, quality, conditions, functions));
    }

    public static LootPoolSingletonContainer.Builder<?> inlineLootTable(LootTable table) {
        return NestedLootTable.simpleBuilder((weight, quality, conditions, functions) -> new NestedLootTable((Either<ResourceKey<LootTable>, LootTable>)Either.right((Object)table), weight, quality, conditions, functions));
    }
}

