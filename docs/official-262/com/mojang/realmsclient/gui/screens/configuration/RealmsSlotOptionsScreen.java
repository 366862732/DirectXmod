/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RealmsSlotOptionsScreen
extends RealmsScreen {
    private static final int DEFAULT_DIFFICULTY = 2;
    public static final List<Difficulty> DIFFICULTIES = ImmutableList.of((Object)Difficulty.PEACEFUL, (Object)Difficulty.EASY, (Object)Difficulty.NORMAL, (Object)Difficulty.HARD);
    private static final int DEFAULT_GAME_MODE = 0;
    public static final List<GameType> GAME_MODES = ImmutableList.of((Object)GameType.SURVIVAL, (Object)GameType.CREATIVE, (Object)GameType.ADVENTURE);
    private static final Component TITLE = Component.translatable("mco.configure.world.buttons.options");
    private static final Component WORLD_NAME_EDIT_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
    private static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
    private static final Component GAME_MODE_BUTTON = Component.translatable("selectWorld.gameMode");
    private static final Component DIFFICULTY_BUTTON = Component.translatable("options.difficulty");
    private static final Component FORCE_GAME_MODE_BUTTON = Component.translatable("mco.configure.world.forceGameMode");
    private static final int SPACING = 8;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final RealmsConfigureWorldScreen parentScreen;
    private final RealmsSlot slot;
    private final RealmsServer.WorldType worldType;
    private final String defaultSlotName;
    private int spawnProtection;
    private boolean forceGameMode;
    private Difficulty difficulty;
    private GameType gameMode;
    private String worldName;
    private @Nullable StringWidget warningHeader;
    private @Nullable SettingsSlider spawnProtectionButton;

    public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen configureWorldScreen, RealmsSlot slot, RealmsServer.WorldType worldType, int activeSlot) {
        super(TITLE);
        this.parentScreen = configureWorldScreen;
        this.slot = slot;
        this.worldType = worldType;
        this.difficulty = RealmsSlotOptionsScreen.findByIndex(DIFFICULTIES, slot.options.difficulty, 2);
        this.gameMode = RealmsSlotOptionsScreen.findByIndex(GAME_MODES, slot.options.gameMode, 0);
        this.defaultSlotName = slot.options.getDefaultSlotName(activeSlot);
        this.setWorldName(slot.options.getSlotName(activeSlot));
        if (worldType == RealmsServer.WorldType.NORMAL) {
            this.spawnProtection = slot.options.spawnProtection;
            this.forceGameMode = slot.options.forceGameMode;
        } else {
            this.spawnProtection = 0;
            this.forceGameMode = false;
        }
    }

    @Override
    public void init() {
        MutableComponent warning;
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(TITLE, this.minecraft.font));
        switch (this.worldType) {
            case ADVENTUREMAP: {
                MutableComponent mutableComponent = Component.translatable("mco.configure.world.edit.subscreen.adventuremap").withColor(-65536);
                break;
            }
            case INSPIRATION: {
                MutableComponent mutableComponent = Component.translatable("mco.configure.world.edit.subscreen.inspiration").withColor(-65536);
                break;
            }
            case EXPERIENCE: {
                MutableComponent mutableComponent = Component.translatable("mco.configure.world.edit.subscreen.experience").withColor(-65536);
                break;
            }
            default: {
                MutableComponent mutableComponent = warning = null;
            }
        }
        if (warning != null) {
            this.layout.setHeaderHeight(41 + this.font.lineHeight + 8);
            this.warningHeader = header.addChild(new StringWidget(warning, this.font));
        }
        GridLayout contentGrid = this.layout.addToContents(new GridLayout().spacing(8));
        contentGrid.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper rowHelper = contentGrid.createRowHelper(2);
        EditBox worldNameEdit = new EditBox(this.minecraft.font, 0, 0, 150, 20, null, WORLD_NAME_EDIT_LABEL);
        worldNameEdit.setValue(this.worldName);
        worldNameEdit.setResponder(this::setWorldName);
        rowHelper.addChild(CommonLayouts.labeledElement(this.font, worldNameEdit, WORLD_NAME_EDIT_LABEL), 2);
        CycleButton<Difficulty> difficultyCycleButton = rowHelper.addChild(CycleButton.builder(Difficulty::getDisplayName, this.difficulty).withValues((Collection<Difficulty>)DIFFICULTIES).create(0, 0, 150, 20, DIFFICULTY_BUTTON, (cycleButton, value) -> {
            this.difficulty = value;
        }));
        CycleButton<GameType> gameTypeCycleButton = rowHelper.addChild(CycleButton.builder(GameType::getShortDisplayName, this.gameMode).withValues((Collection<GameType>)GAME_MODES).create(0, 0, 150, 20, GAME_MODE_BUTTON, (cycleButton, value) -> {
            this.gameMode = value;
        }));
        CycleButton<Boolean> forceGameModeButton = rowHelper.addChild(CycleButton.onOffBuilder(this.forceGameMode).create(0, 0, 150, 20, FORCE_GAME_MODE_BUTTON, (cycleButton, value) -> {
            this.forceGameMode = value;
        }));
        this.spawnProtectionButton = rowHelper.addChild(new SettingsSlider(this, 0, 0, 150, this.spawnProtection, 0.0f, 16.0f));
        if (this.worldType != RealmsServer.WorldType.NORMAL) {
            this.spawnProtectionButton.active = false;
            forceGameModeButton.active = false;
        }
        if (this.slot.isHardcore()) {
            difficultyCycleButton.active = false;
            gameTypeCycleButton.active = false;
            forceGameModeButton.active = false;
        }
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_CONTINUE, button -> this.saveSettings()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
        RealmsSlotOptionsScreen realmsSlotOptionsScreen = this;
        this.layout.visitWidgets(x$0 -> realmsSlotOptionsScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parentScreen);
    }

    private static <T> T findByIndex(List<T> values, int index, int defaultIndex) {
        if (index < 0 || index >= values.size()) {
            return values.get(defaultIndex);
        }
        return values.get(index);
    }

    private static <T> int findIndex(List<T> values, T value, int defaultIndex) {
        int result = values.indexOf(value);
        return result == -1 ? defaultIndex : result;
    }

    @Override
    public Component getNarrationMessage() {
        if (this.warningHeader == null) {
            return super.getNarrationMessage();
        }
        return CommonComponents.joinForNarration(this.getTitle(), this.warningHeader.getMessage());
    }

    private void setWorldName(String value) {
        this.worldName = value.equals(this.defaultSlotName) ? "" : value;
    }

    private void saveSettings() {
        int difficultyId = RealmsSlotOptionsScreen.findIndex(DIFFICULTIES, this.difficulty, 2);
        int gameModeId = RealmsSlotOptionsScreen.findIndex(GAME_MODES, this.gameMode, 0);
        if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP || this.worldType == RealmsServer.WorldType.EXPERIENCE || this.worldType == RealmsServer.WorldType.INSPIRATION) {
            this.parentScreen.saveSlotSettings(new RealmsSlot(this.slot.slotId, new RealmsWorldOptions(this.slot.options.spawnProtection, difficultyId, gameModeId, this.slot.options.forceGameMode, this.worldName, this.slot.options.version, this.slot.options.compatibility), this.slot.settings));
        } else {
            this.parentScreen.saveSlotSettings(new RealmsSlot(this.slot.slotId, new RealmsWorldOptions(this.spawnProtection, difficultyId, gameModeId, this.forceGameMode, this.worldName, this.slot.options.version, this.slot.options.compatibility), this.slot.settings));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class SettingsSlider
    extends AbstractSliderButton {
        private final double minValue;
        private final double maxValue;
        final /* synthetic */ RealmsSlotOptionsScreen this$0;

        public SettingsSlider(RealmsSlotOptionsScreen realmsSlotOptionsScreen, int x, int y, int width, int currentValue, float minValue, float maxValue) {
            RealmsSlotOptionsScreen realmsSlotOptionsScreen2 = realmsSlotOptionsScreen;
            Objects.requireNonNull(realmsSlotOptionsScreen2);
            this.this$0 = realmsSlotOptionsScreen2;
            super(x, y, width, 20, CommonComponents.EMPTY, 0.0);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.value = (Mth.clamp((float)currentValue, minValue, maxValue) - minValue) / (maxValue - minValue);
            this.updateMessage();
        }

        @Override
        public void applyValue() {
            if (!this.this$0.spawnProtectionButton.active) {
                return;
            }
            this.this$0.spawnProtection = (int)Mth.lerp(Mth.clamp(this.value, 0.0, 1.0), this.minValue, this.maxValue);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(CommonComponents.optionNameValue(SPAWN_PROTECTION_TEXT, this.this$0.spawnProtection == 0 ? CommonComponents.OPTION_OFF : Component.literal(String.valueOf(this.this$0.spawnProtection))));
        }
    }
}

