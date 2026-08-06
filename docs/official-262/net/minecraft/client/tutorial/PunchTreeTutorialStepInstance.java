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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.FindTreeTutorialStepInstance;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialStepInstance;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PunchTreeTutorialStepInstance
implements TutorialStepInstance {
    private static final int HINT_DELAY = 600;
    private static final Component TITLE = Component.translatable("tutorial.punch_tree.title");
    private static final Component DESCRIPTION = Component.translatable("tutorial.punch_tree.description", Tutorial.key("attack"));
    private final Tutorial tutorial;
    private @Nullable TutorialToast toast;
    private int timeWaiting;
    private int resetCount;

    public PunchTreeTutorialStepInstance(Tutorial tutorial) {
        this.tutorial = tutorial;
    }

    @Override
    public void tick() {
        LocalPlayer player;
        ++this.timeWaiting;
        if (!this.tutorial.isSurvival()) {
            this.tutorial.setStep(TutorialSteps.NONE);
            return;
        }
        Minecraft minecraft = this.tutorial.getMinecraft();
        if (this.timeWaiting == 1 && (player = minecraft.player) != null) {
            if (player.getInventory().contains(ItemTags.LOGS)) {
                this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
                return;
            }
            if (FindTreeTutorialStepInstance.hasPunchedTreesPreviously(player)) {
                this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
                return;
            }
        }
        if ((this.timeWaiting >= 600 || this.resetCount > 3) && this.toast == null) {
            this.toast = new TutorialToast(minecraft.font, TutorialToast.Icons.TREE, TITLE, DESCRIPTION, true);
            minecraft.gui.toastManager().addToast(this.toast);
        }
    }

    @Override
    public void clear() {
        if (this.toast != null) {
            this.toast.hide();
            this.toast = null;
        }
    }

    @Override
    public void onDestroyBlock(ClientLevel level, BlockPos pos, BlockState state, float percent) {
        boolean isLogBlock = state.is(BlockTags.LOGS);
        if (isLogBlock && percent > 0.0f) {
            if (this.toast != null) {
                this.toast.updateProgress(percent);
            }
            if (percent >= 1.0f) {
                this.tutorial.setStep(TutorialSteps.OPEN_INVENTORY);
            }
        } else if (this.toast != null) {
            this.toast.updateProgress(0.0f);
        } else if (isLogBlock) {
            ++this.resetCount;
        }
    }

    @Override
    public void onGetItem(ItemStack itemStack) {
        if (itemStack.is(ItemTags.LOGS)) {
            this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
            return;
        }
    }
}

