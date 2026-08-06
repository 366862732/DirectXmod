/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.text2speech.Narrator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client;

import com.mojang.blaze3d.platform.MessageBox;
import com.mojang.logging.LogUtils;
import com.mojang.text2speech.Narrator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class GameNarrator {
    public static final Component NO_TITLE = CommonComponents.EMPTY;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final Narrator narrator = Narrator.getNarrator();

    public GameNarrator(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void sayChatQueued(Component message) {
        if (this.getStatus().shouldNarrateChat()) {
            this.narrateNotInterruptingMessage(message);
        }
    }

    public void saySystemChatQueued(Component message) {
        if (this.getStatus().shouldNarrateSystemOrChat()) {
            this.narrateNotInterruptingMessage(message);
        }
    }

    public void saySystemQueued(Component message) {
        if (this.getStatus().shouldNarrateSystem()) {
            this.narrateNotInterruptingMessage(message);
        }
    }

    private void narrateNotInterruptingMessage(Component message) {
        String messageString = message.getString();
        if (!messageString.isEmpty()) {
            this.logNarratedMessage(messageString);
            this.narrateMessage(messageString, false);
        }
    }

    public void saySystemNow(Component message) {
        this.saySystemNow(message.getString());
    }

    public void saySystemNow(String message) {
        if (this.getStatus().shouldNarrateSystem() && !message.isEmpty()) {
            this.logNarratedMessage(message);
            if (this.narrator.active()) {
                this.narrator.clear();
                this.narrateMessage(message, true);
            }
        }
    }

    private void narrateMessage(String message, boolean interrupt) {
        this.narrator.say(message, interrupt, this.minecraft.options.getFinalSoundSourceVolume(SoundSource.VOICE));
    }

    private NarratorStatus getStatus() {
        return this.minecraft.options.narrator().get();
    }

    private void logNarratedMessage(String message) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            LOGGER.debug("Narrating: {}", (Object)message.replaceAll("\n", "\\\\n"));
        }
    }

    public void updateNarratorStatus(NarratorStatus status) {
        this.clear();
        this.narrateMessage(Component.translatable("options.narrator").append(" : ").append(status.getName()).getString(), true);
        ToastManager toastManager = Minecraft.getInstance().gui.toastManager();
        if (this.narrator.active()) {
            if (status == NarratorStatus.OFF) {
                SystemToast.addOrUpdate(toastManager, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.disabled"), null);
            } else {
                SystemToast.addOrUpdate(toastManager, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.enabled"), status.getName());
            }
        } else {
            SystemToast.addOrUpdate(toastManager, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.disabled"), Component.translatable("options.narrator.notavailable"));
        }
    }

    public boolean isActive() {
        return this.narrator.active();
    }

    public void clear() {
        if (this.getStatus() == NarratorStatus.OFF || !this.narrator.active()) {
            return;
        }
        this.narrator.clear();
    }

    public void destroy() {
        this.narrator.destroy();
    }

    public void checkStatus(boolean requiredActive) {
        if (requiredActive && !this.isActive() && !MessageBox.errorWithContinue("Failed to initialize text-to-speech library. Do you want to continue?\nIf this problem persists, please report it at bugs.mojang.com")) {
            throw new NarratorInitException("Narrator library is not active");
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class NarratorInitException
    extends SilentInitException {
        public NarratorInitException(String message) {
            super(message);
        }
    }
}

