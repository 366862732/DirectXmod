/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerLootTable
extends LootItemConditionalFunction {
    public static final MapCodec<SetContainerLootTable> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetContainerLootTable.commonFields(i).and(i.group((App)LootTable.KEY_CODEC.fieldOf("name").forGetter(f -> f.name), (App)Codec.LONG.optionalFieldOf("seed", (Object)0L).forGetter(f -> f.seed), (App)BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("type").forGetter(f -> f.type))).apply((Applicative)i, SetContainerLootTable::new));
    private final ResourceKey<LootTable> name;
    private final long seed;
    private final Holder<BlockEntityType<?>> type;

    private SetContainerLootTable(List<LootItemCondition> predicates, ResourceKey<LootTable> name, long seed, Holder<BlockEntityType<?>> type) {
        super(predicates);
        this.name = name;
        this.seed = seed;
        this.type = type;
    }

    public MapCodec<SetContainerLootTable> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        if (itemStack.isEmpty()) {
            return itemStack;
        }
        itemStack.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(this.name, this.seed));
        return itemStack;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        if (!context.allowsReferences()) {
            context.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(this.name));
            return;
        }
        if (context.resolver().get(this.name).isEmpty()) {
            context.reportProblem(new ValidationContext.MissingReferenceProblem(this.name));
        }
    }

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> type, ResourceKey<LootTable> value) {
        return SetContainerLootTable.simpleBuilder(conditions -> new SetContainerLootTable((List<LootItemCondition>)conditions, value, 0L, (Holder<BlockEntityType<?>>)type.builtInRegistryHolder()));
    }

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> type, ResourceKey<LootTable> value, long seed) {
        return SetContainerLootTable.simpleBuilder(conditions -> new SetContainerLootTable((List<LootItemCondition>)conditions, value, seed, (Holder<BlockEntityType<?>>)type.builtInRegistryHolder()));
    }
}

