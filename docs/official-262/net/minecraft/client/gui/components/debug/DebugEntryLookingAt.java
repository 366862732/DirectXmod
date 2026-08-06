/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.TypedInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class DebugEntryLookingAt
implements DebugScreenEntry {
    private static final int RANGE = 20;
    private static final Identifier BLOCK_GROUP = Identifier.withDefaultNamespace("looking_at_block");
    private static final Identifier FLUID_GROUP = Identifier.withDefaultNamespace("looking_at_fluid");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Level clientOrServerLevel;
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        Level level = clientOrServerLevel = SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES ? serverOrClientLevel : Minecraft.getInstance().level;
        if (cameraEntity == null || clientOrServerLevel == null) {
            return;
        }
        HitResult block = this.getHitResult(cameraEntity);
        ArrayList<String> result = new ArrayList<String>();
        if (block.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult)block).getBlockPos();
            this.extractInfo(result, clientOrServerLevel, pos);
        }
        displayer.addToGroup(this.group(), result);
    }

    public abstract HitResult getHitResult(Entity var1);

    public abstract void extractInfo(List<String> var1, Level var2, BlockPos var3);

    public abstract Identifier group();

    public static void addTagEntries(List<String> result, TypedInstance<?> instance) {
        instance.tags().map(e -> "#" + String.valueOf(e.location())).forEach(result::add);
    }

    @Environment(value=EnvType.CLIENT)
    public static class FluidTagInfo
    extends DebugEntryLookingAtTags<FluidState> {
        @Override
        public HitResult getHitResult(Entity cameraEntity) {
            return cameraEntity.pick(20.0, 0.0f, true);
        }

        @Override
        public FluidState getInstance(Level level, BlockPos pos) {
            return level.getFluidState(pos);
        }

        @Override
        public Identifier group() {
            return FLUID_GROUP;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class FluidStateInfo
    extends DebugEntryLookingAtState<Fluid, FluidState> {
        protected FluidStateInfo() {
            super("Targeted Fluid");
        }

        @Override
        public HitResult getHitResult(Entity cameraEntity) {
            return cameraEntity.pick(20.0, 0.0f, true);
        }

        @Override
        public FluidState getInstance(Level level, BlockPos pos) {
            return level.getFluidState(pos);
        }

        @Override
        public Identifier group() {
            return FLUID_GROUP;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class BlockTagInfo
    extends DebugEntryLookingAtTags<BlockState> {
        @Override
        public HitResult getHitResult(Entity cameraEntity) {
            return cameraEntity.pick(20.0, 0.0f, false);
        }

        @Override
        public BlockState getInstance(Level level, BlockPos pos) {
            return level.getBlockState(pos);
        }

        @Override
        public Identifier group() {
            return BLOCK_GROUP;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class BlockStateInfo
    extends DebugEntryLookingAtState<Block, BlockState> {
        protected BlockStateInfo() {
            super("Targeted Block");
        }

        @Override
        public HitResult getHitResult(Entity cameraEntity) {
            return cameraEntity.pick(20.0, 0.0f, false);
        }

        @Override
        public BlockState getInstance(Level level, BlockPos pos) {
            return level.getBlockState(pos);
        }

        @Override
        public Identifier group() {
            return BLOCK_GROUP;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class DebugEntryLookingAtTags<T extends TypedInstance<?>>
    extends DebugEntryLookingAt {
        protected abstract T getInstance(Level var1, BlockPos var2);

        @Override
        public void extractInfo(List<String> result, Level level, BlockPos pos) {
            T instance = this.getInstance(level, pos);
            DebugEntryLookingAtTags.addTagEntries(result, instance);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class DebugEntryLookingAtState<OwnerType, StateType extends StateHolder<OwnerType, StateType>>
    extends DebugEntryLookingAt {
        private final String prefix;

        protected DebugEntryLookingAtState(String prefix) {
            this.prefix = prefix;
        }

        protected abstract StateType getInstance(Level var1, BlockPos var2);

        @Override
        public void extractInfo(List<String> result, Level level, BlockPos pos) {
            StateType stateInstance = this.getInstance(level, pos);
            result.add(String.valueOf((Object)ChatFormatting.UNDERLINE) + this.prefix + ": " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            result.add(((TypedInstance)stateInstance).typeHolder().getRegisteredName());
            DebugEntryLookingAtState.addStateProperties(result, stateInstance);
        }

        private static void addStateProperties(List<String> result, StateHolder<?, ?> stateHolder) {
            stateHolder.getValues().forEach(entry -> result.add(DebugEntryLookingAtState.getPropertyValueString(entry)));
        }

        private static String getPropertyValueString(Property.Value<?> entry) {
            Object valueString = entry.valueName();
            if (Boolean.TRUE.equals(entry.value())) {
                valueString = String.valueOf((Object)ChatFormatting.GREEN) + (String)valueString;
            } else if (Boolean.FALSE.equals(entry.value())) {
                valueString = String.valueOf((Object)ChatFormatting.RED) + (String)valueString;
            }
            return entry.property().getName() + ": " + (String)valueString;
        }
    }
}

