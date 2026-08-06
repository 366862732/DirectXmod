/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gamerules.GameRules;

public class InfestedBlock
extends Block {
    public static final MapCodec<InfestedBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BuiltInRegistries.BLOCK.byNameCodec().fieldOf("host").forGetter(InfestedBlock::getHostBlock), InfestedBlock.propertiesCodec()).apply((Applicative)i, InfestedBlock::new));
    private final Block hostBlock;
    private static final Map<Block, Block> BLOCK_BY_HOST_BLOCK = Maps.newIdentityHashMap();
    private static final Map<BlockState, BlockState> HOST_TO_INFESTED_STATES = Maps.newIdentityHashMap();
    private static final Map<BlockState, BlockState> INFESTED_TO_HOST_STATES = Maps.newIdentityHashMap();

    public MapCodec<? extends InfestedBlock> codec() {
        return CODEC;
    }

    public InfestedBlock(Block hostBlock, BlockBehaviour.Properties properties) {
        super(properties.destroyTime(hostBlock.defaultDestroyTime() / 2.0f).explosionResistance(0.75f));
        this.hostBlock = hostBlock;
        BLOCK_BY_HOST_BLOCK.put(hostBlock, this);
    }

    public Block getHostBlock() {
        return this.hostBlock;
    }

    public static boolean isCompatibleHostBlock(BlockState blockState) {
        return BLOCK_BY_HOST_BLOCK.containsKey(blockState.getBlock());
    }

    private void spawnInfestation(ServerLevel level, BlockPos pos) {
        Silverfish silverfish = EntityTypes.SILVERFISH.create((Level)level, EntitySpawnReason.TRIGGERED);
        if (silverfish != null) {
            silverfish.snapTo((double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(silverfish);
            silverfish.spawnAnim();
        }
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (level.getGameRules().get(GameRules.BLOCK_DROPS).booleanValue() && !EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
            this.spawnInfestation(level, pos);
        }
    }

    public static BlockState infestedStateByHost(BlockState hostState) {
        return InfestedBlock.getNewStateWithProperties(HOST_TO_INFESTED_STATES, hostState, () -> BLOCK_BY_HOST_BLOCK.get(hostState.getBlock()).defaultBlockState());
    }

    public BlockState hostStateByInfested(BlockState infestedState) {
        return InfestedBlock.getNewStateWithProperties(INFESTED_TO_HOST_STATES, infestedState, () -> this.getHostBlock().defaultBlockState());
    }

    private static BlockState getNewStateWithProperties(Map<BlockState, BlockState> map, BlockState oldState, Supplier<BlockState> newStateSupplier) {
        return map.computeIfAbsent(oldState, k -> {
            BlockState newState = (BlockState)newStateSupplier.get();
            for (Property<?> property : k.getProperties()) {
                newState = InfestedBlock.copyProperty(property, k, newState);
            }
            return newState;
        });
    }

    private static <T extends Comparable<T>> BlockState copyProperty(Property<T> property, BlockState source, BlockState target) {
        return target.hasProperty(property) ? (BlockState)target.setValue(property, source.getValue(property)) : target;
    }
}

