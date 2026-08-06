/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SignBlockEntity
extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TEXT_LINE_WIDTH = 90;
    private static final int TEXT_LINE_HEIGHT = 10;
    private static final boolean DEFAULT_IS_WAXED = false;
    private @Nullable UUID playerWhoMayEdit;
    private SignText frontText = this.createDefaultSignText();
    private SignText backText = this.createDefaultSignText();
    private boolean isWaxed = false;

    public SignBlockEntity(BlockPos worldPosition, BlockState blockState) {
        this((BlockEntityType<? extends SignBlockEntity>)BlockEntityTypes.SIGN, worldPosition, blockState);
    }

    public SignBlockEntity(BlockEntityType<? extends SignBlockEntity> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }

    protected SignText createDefaultSignText() {
        return new SignText();
    }

    public boolean isFacingFrontText(Player player) {
        Block block = this.getBlockState().getBlock();
        if (block instanceof SignBlock) {
            float playerYRot;
            SignBlock sign = (SignBlock)block;
            Vec3 signPositionOffset = sign.getSignHitboxCenterPosition(this.getBlockState());
            double xd = player.getX() - ((double)this.getBlockPos().getX() + signPositionOffset.x);
            double zd = player.getZ() - ((double)this.getBlockPos().getZ() + signPositionOffset.z);
            float signYRot = sign.getYRotationDegrees(this.getBlockState());
            return Mth.degreesDifferenceAbs(signYRot, playerYRot = (float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f) <= 90.0f;
        }
        return false;
    }

    public SignText getText(boolean isFrontText) {
        return isFrontText ? this.frontText : this.backText;
    }

    public SignText getFrontText() {
        return this.frontText;
    }

    public SignText getBackText() {
        return this.backText;
    }

    public int getTextLineHeight() {
        return 10;
    }

    public int getMaxTextLineWidth() {
        return 90;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("front_text", SignText.DIRECT_CODEC, this.frontText);
        output.store("back_text", SignText.DIRECT_CODEC, this.backText);
        output.putBoolean("is_waxed", this.isWaxed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.frontText = input.read("front_text", SignText.DIRECT_CODEC).map(this::loadLines).orElseGet(SignText::new);
        this.backText = input.read("back_text", SignText.DIRECT_CODEC).map(this::loadLines).orElseGet(SignText::new);
        this.isWaxed = input.getBooleanOr("is_waxed", false);
    }

    private SignText loadLines(SignText data) {
        for (int i = 0; i < 4; ++i) {
            Component unfilteredMessage = this.loadLine(data.getMessage(i, false));
            Component filteredMessage = this.loadLine(data.getMessage(i, true));
            data = data.setMessage(i, unfilteredMessage, filteredMessage);
        }
        return data;
    }

    private Component loadLine(Component component) {
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            try {
                return ComponentUtils.resolve(ResolutionContext.create(SignBlockEntity.createCommandSourceStack(null, serverLevel, this.worldPosition)), component);
            }
            catch (CommandSyntaxException commandSyntaxException) {
                // empty catch block
            }
        }
        return component;
    }

    public void updateSignText(Player player, boolean frontText, List<FilteredText> lines) {
        if (this.isWaxed() || !player.getUUID().equals(this.getPlayerWhoMayEdit()) || this.level == null) {
            LOGGER.warn("Player {} just tried to change non-editable sign", (Object)player.getPlainTextName());
            return;
        }
        this.updateText(text -> this.setMessages(player, lines, (SignText)text), frontText);
        this.setAllowedPlayerEditor(null);
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public boolean updateText(UnaryOperator<SignText> function, boolean isFrontText) {
        SignText text = this.getText(isFrontText);
        return this.setText((SignText)function.apply(text), isFrontText);
    }

    private SignText setMessages(Player player, List<FilteredText> lines, SignText text) {
        for (int i = 0; i < lines.size(); ++i) {
            FilteredText line = lines.get(i);
            Style currentTextStyle = text.getMessage(i, player.isTextFilteringEnabled()).getStyle();
            text = player.isTextFilteringEnabled() ? text.setMessage(i, Component.literal(line.filteredOrEmpty()).setStyle(currentTextStyle)) : text.setMessage(i, Component.literal(line.raw()).setStyle(currentTextStyle), Component.literal(line.filteredOrEmpty()).setStyle(currentTextStyle));
        }
        return text;
    }

    public boolean setText(SignText text, boolean isFrontText) {
        return isFrontText ? this.setFrontText(text) : this.setBackText(text);
    }

    private boolean setBackText(SignText text) {
        if (text != this.backText) {
            this.backText = text;
            this.markUpdated();
            return true;
        }
        return false;
    }

    private boolean setFrontText(SignText text) {
        if (text != this.frontText) {
            this.frontText = text;
            this.markUpdated();
            return true;
        }
        return false;
    }

    public boolean canExecuteClickCommands(boolean isFrontText, Player player) {
        return this.isWaxed() && this.getText(isFrontText).hasAnyClickCommands(player);
    }

    public boolean executeClickCommandsIfPresent(ServerLevel level, Player player, BlockPos pos, boolean isFrontText) {
        boolean hasAnyClickCommand = false;
        block5: for (Component message : this.getText(isFrontText).getMessages(player.isTextFilteringEnabled())) {
            ClickEvent event;
            Style style = message.getStyle();
            ClickEvent clickEvent = event = style.getClickEvent();
            int n = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ClickEvent.RunCommand.class, ClickEvent.ShowDialog.class, ClickEvent.Custom.class}, (ClickEvent)clickEvent, n)) {
                case 0: {
                    ClickEvent.RunCommand command = (ClickEvent.RunCommand)clickEvent;
                    level.getServer().getCommands().performPrefixedCommand(SignBlockEntity.createCommandSourceStack(player, level, pos), command.command());
                    hasAnyClickCommand = true;
                    continue block5;
                }
                case 1: {
                    ClickEvent.ShowDialog dialog = (ClickEvent.ShowDialog)clickEvent;
                    player.openDialog(dialog.dialog());
                    hasAnyClickCommand = true;
                    continue block5;
                }
                case 2: {
                    ClickEvent.Custom custom = (ClickEvent.Custom)clickEvent;
                    level.getServer().handleCustomClickAction(custom.id(), custom.payload());
                    hasAnyClickCommand = true;
                    continue block5;
                }
            }
        }
        return hasAnyClickCommand;
    }

    private static CommandSourceStack createCommandSourceStack(@Nullable Player player, ServerLevel level, BlockPos pos) {
        String textName = player == null ? "Sign" : player.getPlainTextName();
        Component displayName = player == null ? Component.literal("Sign") : player.getDisplayName();
        return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(pos), Vec2.ZERO, level, LevelBasedPermissionSet.GAMEMASTER, textName, displayName, level.getServer(), player);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public void setAllowedPlayerEditor(@Nullable UUID playerUUID) {
        this.playerWhoMayEdit = playerUUID;
    }

    public @Nullable UUID getPlayerWhoMayEdit() {
        return this.playerWhoMayEdit;
    }

    private void markUpdated() {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public boolean isWaxed() {
        return this.isWaxed;
    }

    public boolean setWaxed(boolean isWaxed) {
        if (this.isWaxed != isWaxed) {
            this.isWaxed = isWaxed;
            this.markUpdated();
            return true;
        }
        return false;
    }

    public boolean playerIsTooFarAwayToEdit(UUID player) {
        Player editingPlayer = this.level.getPlayerByUUID(player);
        return editingPlayer == null || !editingPlayer.isWithinBlockInteractionRange(this.getBlockPos(), 4.0);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, SignBlockEntity signBlockEntity) {
        UUID playerWhoMayEdit = signBlockEntity.getPlayerWhoMayEdit();
        if (playerWhoMayEdit != null) {
            signBlockEntity.clearInvalidPlayerWhoMayEdit(signBlockEntity, level, playerWhoMayEdit);
        }
    }

    private void clearInvalidPlayerWhoMayEdit(SignBlockEntity signBlockEntity, Level level, UUID playerWhoMayEdit) {
        if (signBlockEntity.playerIsTooFarAwayToEdit(playerWhoMayEdit)) {
            signBlockEntity.setAllowedPlayerEditor(null);
        }
    }

    public SoundEvent getSignInteractionFailedSoundEvent() {
        return SoundEvents.WAXED_SIGN_INTERACT_FAIL;
    }
}

