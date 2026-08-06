/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class DemoMode
extends ServerPlayerGameMode {
    public static final int DEMO_DAYS = 5;
    public static final int TOTAL_PLAY_TICKS = 120500;
    private boolean displayedIntro;
    private boolean demoHasEnded;
    private int demoEndedReminder;
    private int gameModeTicks;

    public DemoMode(ServerPlayer player) {
        super(player);
    }

    @Override
    public void tick() {
        super.tick();
        ++this.gameModeTicks;
        long time = this.level.getGameTime();
        long day = time / 24000L + 1L;
        if (!this.displayedIntro && this.gameModeTicks > 20) {
            this.displayedIntro = true;
            this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 0.0f));
        }
        boolean bl = this.demoHasEnded = time > 120500L;
        if (this.demoHasEnded) {
            ++this.demoEndedReminder;
        }
        if (time % 24000L == 500L) {
            if (day <= 6L) {
                if (day == 6L) {
                    this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 104.0f));
                } else {
                    this.player.sendSystemMessage(Component.translatable("demo.day." + day));
                }
            }
        } else if (day == 1L) {
            if (time == 100L) {
                this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 101.0f));
            } else if (time == 175L) {
                this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 102.0f));
            } else if (time == 250L) {
                this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 103.0f));
            }
        } else if (day == 5L && time % 24000L == 22000L) {
            this.player.sendSystemMessage(Component.translatable("demo.day.warning"));
        }
    }

    private void outputDemoReminder() {
        if (this.demoEndedReminder > 100) {
            this.player.sendSystemMessage(Component.translatable("demo.reminder"));
            this.demoEndedReminder = 0;
        }
    }

    @Override
    public void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int maxY, int sequence) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
            return;
        }
        super.handleBlockBreakAction(pos, action, direction, maxY, sequence);
    }

    @Override
    public InteractionResult useItem(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
            return InteractionResult.PASS;
        }
        return super.useItem(player, level, itemStack, hand);
    }

    @Override
    public InteractionResult useItemOn(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, BlockHitResult hitResult) {
        if (this.demoHasEnded) {
            this.outputDemoReminder();
            return InteractionResult.PASS;
        }
        return super.useItemOn(player, level, itemStack, hand, hitResult);
    }
}

