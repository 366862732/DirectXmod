/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.RealmsTextureManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RealmsWorldSlotButton
extends Button {
    private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
    public static final Identifier EMPTY_SLOT_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/empty_frame.png");
    public static final Identifier DEFAULT_WORLD_SLOT_1 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_0.png");
    public static final Identifier DEFAULT_WORLD_SLOT_2 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_2.png");
    public static final Identifier DEFAULT_WORLD_SLOT_3 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_3.png");
    private static final Component SWITCH_TO_MINIGAME_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.minigame");
    private static final Component SWITCH_TO_WORLD_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip");
    private static final Component MINIGAME = Component.translatable("mco.worldSlot.minigame");
    private final RealmsConfigureWorldScreen configureWorldScreen;
    private final int slotIndex;
    private final StringWidget slotNameWidget;
    private State state;

    public RealmsWorldSlotButton(RealmsConfigureWorldScreen configureWorldScreen, int x, int y, int width, int height, int slotIndex, RealmsServer serverData, Button.OnPress onPress) {
        super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.configureWorldScreen = configureWorldScreen;
        this.slotIndex = slotIndex;
        this.state = this.setServerData(serverData);
        this.slotNameWidget = new StringWidget(Component.literal(this.state.slotName), Minecraft.getInstance().font);
    }

    public State getState() {
        return this.state;
    }

    public State setServerData(RealmsServer serverData) {
        this.state = new State(serverData, this.slotIndex);
        this.setTooltipAndNarration(this.state, serverData.minigameName);
        return this.state;
    }

    private void setTooltipAndNarration(State state, @Nullable String minigameName) {
        Component tooltipComponent;
        switch (state.action.ordinal()) {
            case 1: {
                Component component;
                if (state.minigame) {
                    component = SWITCH_TO_MINIGAME_SLOT_TOOLTIP;
                    break;
                }
                component = SWITCH_TO_WORLD_SLOT_TOOLTIP;
                break;
            }
            default: {
                Component component = tooltipComponent = null;
            }
        }
        if (tooltipComponent != null) {
            this.setTooltip(Tooltip.create(tooltipComponent));
        }
        MutableComponent slotContents = Component.literal(state.slotName);
        if (state.minigame && minigameName != null) {
            slotContents = slotContents.append(CommonComponents.SPACE).append(minigameName);
        }
        this.setMessage(slotContents);
    }

    private static Action getAction(boolean activeSlot, boolean empty, boolean expired) {
        if (!(activeSlot || empty && expired)) {
            return Action.SWITCH_SLOT;
        }
        return Action.NOTHING;
    }

    @Override
    public boolean isActive() {
        return this.state.action != Action.NOTHING && super.isActive();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int x = this.getX();
        int y = this.getY();
        boolean hoveredOrFocused = this.isHoveredOrFocused();
        Identifier texture = this.state.minigame ? RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image) : (this.state.empty ? EMPTY_SLOT_LOCATION : (this.state.image != null && this.state.imageId != -1L ? RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image) : (this.slotIndex == 1 ? DEFAULT_WORLD_SLOT_1 : (this.slotIndex == 2 ? DEFAULT_WORLD_SLOT_2 : (this.slotIndex == 3 ? DEFAULT_WORLD_SLOT_3 : EMPTY_SLOT_LOCATION)))));
        int color = -1;
        if (!this.state.activeSlot) {
            color = ARGB.colorFromFloat(1.0f, 0.56f, 0.56f, 0.56f);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + 1, y + 1, 0.0f, 0.0f, this.width - 2, this.height - 2, 74, 74, 74, 74, color);
        if (hoveredOrFocused && this.state.action != Action.NOTHING) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, this.width, this.height);
        } else if (this.state.activeSlot) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, this.width, this.height, ARGB.colorFromFloat(1.0f, 0.8f, 0.8f, 0.8f));
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, this.width, this.height, ARGB.colorFromFloat(1.0f, 0.56f, 0.56f, 0.56f));
        }
        if (this.state.hardcore) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_MODE_SPRITE, x + 3, y + 4, 9, 8);
        }
        this.slotNameWidget.setMaxWidth(this.getWidth() - (this.state.activeSlot ? 2 : 0), StringWidget.TextOverflow.SCROLLING);
        this.slotNameWidget.setPosition(this.getX() + this.getWidth() / 2 - this.slotNameWidget.getWidth() / 2, y + this.height - 14);
        this.slotNameWidget.extractRenderState(graphics, mouseX, mouseY, a);
        if (this.state.activeSlot) {
            graphics.centeredText(Minecraft.getInstance().font, RealmsMainScreen.getVersionComponent(this.state.slotVersion, this.state.compatibility.isCompatible()), x + this.width / 2, y + this.height + 2, -1);
        }
    }

    @Override
    protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (Minecraft.getInstance().gui.screen() == this.configureWorldScreen) {
            super.extractTooltipForNextRenderPass(graphics, mouseX, mouseY);
        }
    }

    public void updateSlotState(RealmsServer serverData) {
        this.state = this.setServerData(serverData);
        this.slotNameWidget.setMessage(Component.literal(this.state.slotName));
    }

    @Environment(value=EnvType.CLIENT)
    public static class State {
        private final String slotName;
        private final String slotVersion;
        private final RealmsServer.Compatibility compatibility;
        private final long imageId;
        private final @Nullable String image;
        public final boolean empty;
        public final boolean minigame;
        public final Action action;
        public final boolean hardcore;
        public final boolean activeSlot;

        public State(RealmsServer serverData, int slotIndex) {
            boolean bl = this.minigame = slotIndex == 4;
            if (this.minigame) {
                this.slotName = MINIGAME.getString();
                this.imageId = serverData.minigameId;
                this.image = serverData.minigameImage;
                this.empty = serverData.minigameId == -1;
                this.slotVersion = "";
                this.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
                this.hardcore = false;
                this.activeSlot = serverData.isMinigameActive();
            } else {
                RealmsSlot slot = serverData.slots.get(slotIndex);
                this.slotName = slot.options.getSlotName(slotIndex);
                this.imageId = slot.options.templateId;
                this.image = slot.options.templateImage;
                this.empty = slot.options.empty;
                this.slotVersion = slot.options.version;
                this.compatibility = slot.options.compatibility;
                this.hardcore = slot.isHardcore();
                this.activeSlot = serverData.activeSlot == slotIndex && !serverData.isMinigameActive();
            }
            this.action = RealmsWorldSlotButton.getAction(this.activeSlot, this.empty, serverData.expired);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Action {
        NOTHING,
        SWITCH_SLOT;

    }
}

