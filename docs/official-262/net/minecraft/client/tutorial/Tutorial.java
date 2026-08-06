/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.tutorial;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.tutorial.TutorialStepInstance;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class Tutorial {
    private final Minecraft minecraft;
    private @Nullable TutorialStepInstance instance;

    public Tutorial(Minecraft minecraft, Options options) {
        this.minecraft = minecraft;
    }

    public void onInput(ClientInput input) {
        if (this.instance != null) {
            this.instance.onInput(input);
        }
    }

    public void onMouse(double xd, double yd) {
        if (this.instance != null) {
            this.instance.onMouse(xd, yd);
        }
    }

    public void onLookAt(@Nullable ClientLevel level, @Nullable HitResult hit) {
        if (this.instance != null && hit != null && level != null) {
            this.instance.onLookAt(level, hit);
        }
    }

    public void onDestroyBlock(ClientLevel level, BlockPos pos, BlockState state, float percent) {
        if (this.instance != null) {
            this.instance.onDestroyBlock(level, pos, state, percent);
        }
    }

    public void onOpenInventory() {
        if (this.instance != null) {
            this.instance.onOpenInventory();
        }
    }

    public void onGetItem(ItemStack itemStack) {
        if (this.instance != null) {
            this.instance.onGetItem(itemStack);
        }
    }

    public void stop() {
        if (this.instance == null) {
            return;
        }
        this.instance.clear();
        this.instance = null;
    }

    public void start() {
        if (this.instance != null) {
            this.stop();
        }
        this.instance = this.minecraft.options.tutorialStep.create(this);
    }

    public void tick() {
        if (this.instance != null) {
            if (this.minecraft.level != null) {
                this.instance.tick();
            } else {
                this.stop();
            }
        } else if (this.minecraft.level != null) {
            this.start();
        }
    }

    public void setStep(TutorialSteps step) {
        this.minecraft.options.tutorialStep = step;
        this.minecraft.options.save();
        if (this.instance != null) {
            this.instance.clear();
            this.instance = step.create(this);
        }
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public boolean isSurvival() {
        if (this.minecraft.gameMode == null) {
            return false;
        }
        return this.minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
    }

    public static Component key(String name) {
        return Component.keybind("key." + name).withStyle(ChatFormatting.BOLD);
    }

    public void onInventoryAction(ItemStack itemCarried, ItemStack itemInSlot, ClickAction clickAction) {
    }
}

