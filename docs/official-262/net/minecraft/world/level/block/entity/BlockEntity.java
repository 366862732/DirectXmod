/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Objects;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BlockEntity
implements DebugValueSource,
TypedInstance<BlockEntityType<?>> {
    private static final Codec<BlockEntityType<?>> TYPE_CODEC = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockEntityType<?> type;
    protected @Nullable Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;
    private DataComponentMap components = DataComponentMap.EMPTY;

    public BlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        this.type = type;
        this.worldPosition = worldPosition.immutable();
        this.validateBlockState(blockState);
        this.blockState = blockState;
    }

    private void validateBlockState(BlockState blockState) {
        if (!this.isValidBlockState(blockState)) {
            throw new IllegalStateException("Invalid block entity " + this.getNameForReporting() + " state at " + String.valueOf(this.worldPosition) + ", got " + String.valueOf(blockState));
        }
    }

    public boolean isValidBlockState(BlockState blockState) {
        return this.type.isValid(blockState);
    }

    public static BlockPos getPosFromTag(ChunkPos base, CompoundTag entityTag) {
        int x = entityTag.getIntOr("x", 0);
        int y = entityTag.getIntOr("y", 0);
        int z = entityTag.getIntOr("z", 0);
        int sectionX = SectionPos.blockToSectionCoord(x);
        int sectionZ = SectionPos.blockToSectionCoord(z);
        if (sectionX != base.x() || sectionZ != base.z()) {
            LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", (Object)entityTag, (Object)base);
            x = base.getBlockX(SectionPos.sectionRelative(x));
            z = base.getBlockZ(SectionPos.sectionRelative(z));
        }
        return new BlockPos(x, y, z);
    }

    public @Nullable Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public boolean hasLevel() {
        return this.level != null;
    }

    protected void loadAdditional(ValueInput input) {
    }

    public final void loadWithComponents(ValueInput input) {
        this.loadAdditional(input);
        this.components = input.read("components", DataComponentMap.CODEC).orElse(DataComponentMap.EMPTY);
    }

    public final void loadCustomOnly(ValueInput input) {
        this.loadAdditional(input);
    }

    protected void saveAdditional(ValueOutput output) {
    }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER);){
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            this.saveWithFullMetadata(output);
            CompoundTag compoundTag = output.buildResult();
            return compoundTag;
        }
    }

    public void saveWithFullMetadata(ValueOutput output) {
        this.saveWithoutMetadata(output);
        this.saveMetadata(output);
    }

    public void saveWithId(ValueOutput output) {
        this.saveWithoutMetadata(output);
        this.saveId(output);
    }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER);){
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            this.saveWithoutMetadata(output);
            CompoundTag compoundTag = output.buildResult();
            return compoundTag;
        }
    }

    public void saveWithoutMetadata(ValueOutput output) {
        this.saveAdditional(output);
        output.store("components", DataComponentMap.CODEC, this.components);
    }

    public final CompoundTag saveCustomOnly(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER);){
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            this.saveCustomOnly(output);
            CompoundTag compoundTag = output.buildResult();
            return compoundTag;
        }
    }

    public void saveCustomOnly(ValueOutput output) {
        this.saveAdditional(output);
    }

    private void saveId(ValueOutput output) {
        BlockEntity.addEntityType(output, this.getType());
    }

    public static void addEntityType(ValueOutput output, BlockEntityType<?> type) {
        output.store("id", TYPE_CODEC, type);
    }

    private void saveMetadata(ValueOutput output) {
        this.saveId(output);
        output.putInt("x", this.worldPosition.getX());
        output.putInt("y", this.worldPosition.getY());
        output.putInt("z", this.worldPosition.getZ());
    }

    public static @Nullable BlockEntity loadStatic(BlockPos pos, BlockState state, CompoundTag tag, HolderLookup.Provider registries) {
        Object entity;
        BlockEntityType type = tag.read("id", TYPE_CODEC).orElse(null);
        if (type == null) {
            LOGGER.error("Skipping block entity with invalid type: {}", (Object)tag.get("id"));
            return null;
        }
        try {
            entity = type.create(pos, state);
        }
        catch (Throwable t) {
            LOGGER.error("Failed to create block entity {} for block {} at position {} ", new Object[]{type, pos, state, t});
            return null;
        }
        ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(((BlockEntity)entity).problemPath(), LOGGER);
        try {
            ((BlockEntity)entity).loadWithComponents(TagValueInput.create((ProblemReporter)reporter, registries, tag));
            Object t = entity;
            reporter.close();
            return t;
        }
        catch (Throwable throwable) {
            try {
                try {
                    reporter.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (Throwable t) {
                LOGGER.error("Failed to load data for block entity {} for block {} at position {}", new Object[]{type, pos, state, t});
                return null;
            }
        }
    }

    public void setChanged() {
        if (this.level != null) {
            BlockEntity.setChanged(this.level, this.worldPosition, this.blockState);
        }
    }

    protected static void setChanged(Level level, BlockPos worldPosition, BlockState blockState) {
        level.blockEntityChanged(worldPosition);
        if (!blockState.isAir()) {
            level.updateNeighbourForOutputSignal(worldPosition, blockState.getBlock());
        }
    }

    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return null;
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
    }

    public void clearRemoved() {
        this.remove = false;
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        BlockEntity blockEntity = this;
        if (blockEntity instanceof Container) {
            Container container = (Container)((Object)blockEntity);
            if (this.level != null) {
                Containers.dropContents(this.level, pos, container);
            }
        }
    }

    public boolean triggerEvent(int b0, int b1) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory category) {
        category.setDetail("Name", this::getNameForReporting);
        category.setDetail("Cached block", this.getBlockState()::toString);
        if (this.level == null) {
            category.setDetail("Block location", () -> String.valueOf(this.worldPosition) + " (world missing)");
        } else {
            category.setDetail("Actual block", this.level.getBlockState(this.worldPosition)::toString);
            CrashReportCategory.populateBlockLocationDetails(category, this.level, this.worldPosition);
        }
    }

    public String getNameForReporting() {
        return this.typeHolder().getRegisteredName() + " // " + this.getClass().getCanonicalName();
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    @Override
    public Holder<BlockEntityType<?>> typeHolder() {
        return this.type.builtInRegistryHolder();
    }

    @Deprecated
    public void setBlockState(BlockState blockState) {
        this.validateBlockState(blockState);
        this.blockState = blockState;
    }

    protected void applyImplicitComponents(DataComponentGetter components) {
    }

    public final void applyComponentsFromItemStack(ItemStack stack) {
        this.applyComponents(stack.getPrototype(), stack.getComponentsPatch());
    }

    public final void applyComponents(DataComponentMap prototype, DataComponentPatch patch) {
        final HashSet<DataComponentType<TooltipProvider>> implicitComponents = new HashSet<DataComponentType<TooltipProvider>>();
        implicitComponents.add(DataComponents.BLOCK_ENTITY_DATA);
        implicitComponents.add(DataComponents.BLOCK_STATE);
        final PatchedDataComponentMap fullView = PatchedDataComponentMap.fromPatch(prototype, patch);
        this.applyImplicitComponents(new DataComponentGetter(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public <T> @Nullable T get(DataComponentType<? extends T> type) {
                implicitComponents.add(type);
                return fullView.get(type);
            }

            @Override
            public <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
                implicitComponents.add(type);
                return fullView.getOrDefault(type, defaultValue);
            }
        });
        DataComponentPatch newPatch = patch.forget(implicitComponents::contains);
        this.components = newPatch.split().added();
    }

    protected void collectImplicitComponents(DataComponentMap.Builder components) {
    }

    @Deprecated
    public void removeComponentsFromTag(ValueOutput output) {
    }

    public final DataComponentMap collectComponents() {
        DataComponentMap.Builder result = DataComponentMap.builder();
        result.addAll(this.components);
        this.collectImplicitComponents(result);
        return result.build();
    }

    public DataComponentMap components() {
        return this.components;
    }

    public void setComponents(DataComponentMap components) {
        this.components = components;
    }

    public static @Nullable Component parseCustomNameSafe(ValueInput input, String name) {
        return input.read(name, ComponentSerialization.CODEC).orElse(null);
    }

    public ProblemReporter.PathElement problemPath() {
        return new BlockEntityPathElement(this);
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
    }

    private record BlockEntityPathElement(BlockEntity blockEntity) implements ProblemReporter.PathElement
    {
        @Override
        public String get() {
            return this.blockEntity.getNameForReporting() + "@" + String.valueOf(this.blockEntity.getBlockPos());
        }
    }
}

