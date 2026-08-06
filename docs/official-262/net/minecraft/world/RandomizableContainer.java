/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface RandomizableContainer
extends Container {
    public static final String LOOT_TABLE_TAG = "LootTable";
    public static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";

    public @Nullable ResourceKey<LootTable> getLootTable();

    public void setLootTable(@Nullable ResourceKey<LootTable> var1);

    default public void setLootTable(ResourceKey<LootTable> lootTable, long seed) {
        this.setLootTable(lootTable);
        this.setLootTableSeed(seed);
    }

    public long getLootTableSeed();

    public void setLootTableSeed(long var1);

    public BlockPos getBlockPos();

    public @Nullable Level getLevel();

    public static void setBlockEntityLootTable(BlockGetter level, RandomSource random, BlockPos blockEntityPos, ResourceKey<LootTable> lootTable) {
        BlockEntity blockEntity = level.getBlockEntity(blockEntityPos);
        if (blockEntity instanceof RandomizableContainer) {
            RandomizableContainer randomizableContainer = (RandomizableContainer)((Object)blockEntity);
            randomizableContainer.setLootTable(lootTable, random.nextLong());
        }
    }

    default public boolean tryLoadLootTable(ValueInput base) {
        ResourceKey lootTable = base.read(LOOT_TABLE_TAG, LootTable.KEY_CODEC).orElse(null);
        this.setLootTable(lootTable);
        this.setLootTableSeed(base.getLongOr(LOOT_TABLE_SEED_TAG, 0L));
        return lootTable != null;
    }

    default public boolean trySaveLootTable(ValueOutput base) {
        ResourceKey<LootTable> lootTable = this.getLootTable();
        if (lootTable == null) {
            return false;
        }
        base.store(LOOT_TABLE_TAG, LootTable.KEY_CODEC, lootTable);
        long lootTableSeed = this.getLootTableSeed();
        if (lootTableSeed != 0L) {
            base.putLong(LOOT_TABLE_SEED_TAG, lootTableSeed);
        }
        return true;
    }

    default public void unpackLootTable(@Nullable Player player) {
        Level level = this.getLevel();
        BlockPos worldPosition = this.getBlockPos();
        ResourceKey<LootTable> lootTableKey = this.getLootTable();
        if (lootTableKey != null && level != null && level.getServer() != null) {
            LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, lootTableKey);
            }
            this.setLootTable(null);
            LootParams.Builder params = new LootParams.Builder((ServerLevel)level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition));
            if (player != null) {
                params.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
            }
            lootTable.fill(this, params.create(LootContextParamSets.CHEST), this.getLootTableSeed());
        }
    }
}

