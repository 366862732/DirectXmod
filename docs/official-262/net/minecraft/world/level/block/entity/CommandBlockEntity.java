/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import java.util.Objects;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CommandBlockEntity
extends BlockEntity {
    private static final boolean DEFAULT_POWERED = false;
    private static final boolean DEFAULT_CONDITION_MET = false;
    private static final boolean DEFAULT_AUTOMATIC = false;
    private boolean powered = false;
    private boolean auto = false;
    private boolean conditionMet = false;
    private final BaseCommandBlock commandBlock = new BaseCommandBlock(this){
        final /* synthetic */ CommandBlockEntity this$0;
        {
            CommandBlockEntity commandBlockEntity = this$0;
            Objects.requireNonNull(commandBlockEntity);
            this.this$0 = commandBlockEntity;
        }

        @Override
        public void setCommand(String command) {
            super.setCommand(command);
            this.this$0.setChanged();
        }

        @Override
        public void onUpdated(ServerLevel level) {
            BlockState state = level.getBlockState(this.this$0.worldPosition);
            level.sendBlockUpdated(this.this$0.worldPosition, state, state, 3);
        }

        @Override
        public CommandSourceStack createCommandSourceStack(ServerLevel level, CommandSource source) {
            Direction facing = this.this$0.getBlockState().getValue(CommandBlock.FACING);
            return new CommandSourceStack(source, Vec3.atCenterOf(this.this$0.worldPosition), new Vec2(0.0f, facing.toYRot()), level, LevelBasedPermissionSet.GAMEMASTER, this.getName().getString(), this.getName(), level.getServer(), null);
        }

        @Override
        public boolean isValid() {
            return !this.this$0.isRemoved();
        }
    };

    public CommandBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.COMMAND_BLOCK, worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.commandBlock.save(output);
        output.putBoolean("powered", this.isPowered());
        output.putBoolean("conditionMet", this.wasConditionMet());
        output.putBoolean("auto", this.isAutomatic());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.commandBlock.load(input);
        this.powered = input.getBooleanOr("powered", false);
        this.conditionMet = input.getBooleanOr("conditionMet", false);
        this.setAutomatic(input.getBooleanOr("auto", false));
    }

    public BaseCommandBlock getCommandBlock() {
        return this.commandBlock;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public boolean isPowered() {
        return this.powered;
    }

    public boolean isAutomatic() {
        return this.auto;
    }

    public void setAutomatic(boolean auto) {
        boolean previousAuto = this.auto;
        this.auto = auto;
        if (!previousAuto && auto && !this.powered && this.level != null && this.getMode() != Mode.SEQUENCE) {
            this.scheduleTick();
        }
    }

    public void onModeSwitch() {
        Mode newMode = this.getMode();
        if (newMode == Mode.AUTO && (this.powered || this.auto) && this.level != null) {
            this.scheduleTick();
        }
    }

    private void scheduleTick() {
        Block commandBlock = this.getBlockState().getBlock();
        if (commandBlock instanceof CommandBlock) {
            this.markConditionMet();
            this.level.scheduleTick(this.worldPosition, commandBlock, 1);
        }
    }

    public boolean wasConditionMet() {
        return this.conditionMet;
    }

    public boolean markConditionMet() {
        this.conditionMet = true;
        if (this.isConditional()) {
            CommandBlockEntity commandBlockEntity;
            BlockEntity backsideCommandBlock;
            BlockPos relative = this.worldPosition.relative(this.level.getBlockState(this.worldPosition).getValue(CommandBlock.FACING).getOpposite());
            this.conditionMet = this.level.getBlockState(relative).getBlock() instanceof CommandBlock ? (backsideCommandBlock = this.level.getBlockEntity(relative)) instanceof CommandBlockEntity && (commandBlockEntity = (CommandBlockEntity)backsideCommandBlock).getCommandBlock().getSuccessCount() > 0 : false;
        }
        return this.conditionMet;
    }

    public Mode getMode() {
        BlockState state = this.getBlockState();
        if (state.is(Blocks.COMMAND_BLOCK)) {
            return Mode.REDSTONE;
        }
        if (state.is(Blocks.REPEATING_COMMAND_BLOCK)) {
            return Mode.AUTO;
        }
        if (state.is(Blocks.CHAIN_COMMAND_BLOCK)) {
            return Mode.SEQUENCE;
        }
        return Mode.REDSTONE;
    }

    public boolean isConditional() {
        BlockState blockState = this.level.getBlockState(this.getBlockPos());
        if (blockState.getBlock() instanceof CommandBlock) {
            return blockState.getValue(CommandBlock.CONDITIONAL);
        }
        return false;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.commandBlock.setCustomName(components.get(DataComponents.CUSTOM_NAME));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.commandBlock.getCustomName());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("CustomName");
        output.discard("conditionMet");
        output.discard("powered");
    }

    public static enum Mode {
        SEQUENCE,
        AUTO,
        REDSTONE;

    }
}

