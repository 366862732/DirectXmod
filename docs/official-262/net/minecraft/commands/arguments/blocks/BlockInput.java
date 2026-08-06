/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.commands.arguments.blocks;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BlockInput
implements Predicate<BlockInWorld> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockState state;
    private final Set<Property<?>> properties;
    private final @Nullable CompoundTag tag;

    public BlockInput(BlockState state, Set<Property<?>> properties, @Nullable CompoundTag tag) {
        this.state = state;
        this.properties = properties;
        this.tag = tag;
    }

    public BlockState getState() {
        return this.state;
    }

    public Set<Property<?>> getDefinedProperties() {
        return this.properties;
    }

    @Override
    public boolean test(BlockInWorld blockInWorld) {
        BlockState state = blockInWorld.getState();
        if (!state.is(this.state.getBlock())) {
            return false;
        }
        for (Property<?> property : this.properties) {
            if (state.getValue(property) == this.state.getValue(property)) continue;
            return false;
        }
        if (this.tag != null) {
            BlockEntity entity = blockInWorld.getEntity();
            return entity != null && NbtUtils.compareNbt(this.tag, entity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
        }
        return true;
    }

    public boolean test(ServerLevel level, BlockPos pos) {
        return this.test(new BlockInWorld(level, pos, false));
    }

    public boolean place(ServerLevel level, BlockPos pos, @Block.UpdateFlags int update) {
        BlockEntity entity;
        BlockState state;
        BlockState blockState = state = (update & 0x10) != 0 ? this.state : Block.updateFromNeighbourShapes(this.state, level, pos);
        if (state.isAir()) {
            state = this.state;
        }
        state = this.overwriteWithDefinedProperties(state);
        boolean affected = false;
        if (level.setBlock(pos, state, update)) {
            affected = true;
        }
        if (this.tag != null && (entity = level.getBlockEntity(pos)) != null) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
                RegistryAccess registries = level.registryAccess();
                ProblemReporter blockEntityReporter = reporter.forChild(entity.problemPath());
                TagValueOutput initialOutput = TagValueOutput.createWithContext(blockEntityReporter.forChild(() -> "(before)"), registries);
                entity.saveWithoutMetadata(initialOutput);
                CompoundTag before = initialOutput.buildResult();
                entity.loadWithComponents(TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)registries, this.tag));
                TagValueOutput updatedOutput = TagValueOutput.createWithContext(blockEntityReporter.forChild(() -> "(after)"), registries);
                entity.saveWithoutMetadata(updatedOutput);
                CompoundTag after = updatedOutput.buildResult();
                if (!after.equals(before)) {
                    affected = true;
                    entity.setChanged();
                    level.getChunkSource().blockChanged(pos);
                }
            }
        }
        return affected;
    }

    private BlockState overwriteWithDefinedProperties(BlockState state) {
        if (state == this.state) {
            return state;
        }
        for (Property<?> property : this.properties) {
            state = BlockInput.copyProperty(state, this.state, property);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState source, Property<T> property) {
        return (BlockState)target.trySetValue(property, source.getValue(property));
    }
}

