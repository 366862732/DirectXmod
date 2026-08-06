/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.options;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;

@Environment(value=EnvType.CLIENT)
public record DifficultyButtons(LayoutElement layout, CycleButton<Difficulty> difficultyButton, LockIconButton lockButton, Level level) {
    public static DifficultyButtons create(Minecraft minecraft, Level level, Screen screen) {
        CycleButton<Difficulty> difficultyButton = CycleButton.builder(Difficulty::getDisplayName, level.getDifficulty()).withValues((Difficulty[])Difficulty.values()).create(0, 0, 150, 20, Component.translatable("options.difficulty"), (button, value) -> minecraft.getConnection().send(new ServerboundChangeDifficultyPacket((Difficulty)value)));
        LockIconButton lockButton = new LockIconButton(0, 0, button -> minecraft.gui.setScreen(new ConfirmScreen(result -> DifficultyButtons.onLockCallback(result, minecraft, screen, difficultyButton, (LockIconButton)button), Component.translatable("difficulty.lock.title"), (Component)Component.translatable("difficulty.lock.question", level.getLevelData().getDifficulty().getDisplayName()))));
        difficultyButton.setWidth(difficultyButton.getWidth() - lockButton.getWidth());
        lockButton.setLocked(DifficultyButtons.isDifficultyLocked(level));
        lockButton.active = !lockButton.isLocked() && DifficultyButtons.playerHasPermissionToChangeDifficulty(minecraft);
        difficultyButton.active = !lockButton.isLocked() && DifficultyButtons.playerHasPermissionToChangeDifficulty(minecraft);
        EqualSpacingLayout linearLayout = new EqualSpacingLayout(150, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
        linearLayout.addChild(difficultyButton);
        linearLayout.addChild(lockButton);
        return new DifficultyButtons(linearLayout, difficultyButton, lockButton, level);
    }

    public void refresh(Minecraft minecraft) {
        this.difficultyButton.setValue(this.level.getDifficulty());
        this.lockButton.setLocked(DifficultyButtons.isDifficultyLocked(this.level));
        this.lockButton.active = !this.lockButton.isLocked() && DifficultyButtons.playerHasPermissionToChangeDifficulty(minecraft);
        this.difficultyButton.active = !this.lockButton.isLocked() && DifficultyButtons.playerHasPermissionToChangeDifficulty(minecraft);
    }

    private static boolean isDifficultyLocked(Level level) {
        return level.getLevelData().isDifficultyLocked() || level.getLevelData().isHardcore();
    }

    private static boolean playerHasPermissionToChangeDifficulty(Minecraft minecraft) {
        return minecraft.hasSingleplayerServer();
    }

    private static void onLockCallback(boolean result, Minecraft minecraft, Screen screen, CycleButton<Difficulty> difficultyButton, LockIconButton lockButton) {
        minecraft.gui.setScreen(screen);
        if (result) {
            minecraft.getConnection().send(new ServerboundLockDifficultyPacket(true));
            lockButton.setLocked(true);
            lockButton.active = false;
            difficultyButton.active = false;
        }
    }
}

