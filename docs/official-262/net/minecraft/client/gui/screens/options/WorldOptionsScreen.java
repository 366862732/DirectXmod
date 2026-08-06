/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.options;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.RestrictionsScreen;
import net.minecraft.client.gui.screens.options.DifficultyButtons;
import net.minecraft.client.gui.screens.options.HasDifficultyReaction;
import net.minecraft.client.gui.screens.options.HasGamemasterPermissionReaction;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class WorldOptionsScreen
extends Screen
implements HasGamemasterPermissionReaction,
HasDifficultyReaction {
    private static final Component TITLE = Component.translatable("options.worldOptions.title");
    private static final Component ALLOW_COMMANDS = Component.translatable("selectWorld.allowCommands");
    private static final Component GAME_MODE = Component.translatable("selectWorld.gameMode");
    private static final Component GAME_RULES = Component.translatable("editGamerule.inGame.button");
    private static final Tooltip GAMERULES_DISABLED_TOOLTIP = Tooltip.create(Component.translatable("editGamerule.inGame.disabled.tooltip"));
    private static final Tooltip GAMERULES_DISABLED_HARDCORE_TOOLTIP = Tooltip.create(Component.translatable("editGamerule.inGame.disabled.hardcore.tooltip"));
    public static final Tooltip GAME_MODE_DISABLED_HARDCORE_TOOLTIP = Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.tooltip"));
    private static final Tooltip GAME_MODE_DISABLED_OPERATOR_TOOLTIP = Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.operator.tooltip"));
    public static final Tooltip ALLOW_COMMANDS_DISABLED_TOOLTIP = Tooltip.create(Component.translatable("options.worldOptions.allow_commands.disabled.tooltip"));
    private static final Component RESTRICTIONS = Component.translatable("restrictions_screen.button");
    private final Screen lastScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final DifficultyButtons difficultyButtons;
    private @Nullable Button gameRulesButton;
    private @Nullable CycleButton<GameType> gameModeButton;

    public WorldOptionsScreen(Screen lastScreen, Level level) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.difficultyButtons = DifficultyButtons.create(this.minecraft, level, this);
    }

    @Override
    protected void init() {
        this.layout.addToHeader(new StringWidget(TITLE, this.font), LayoutSettings::alignHorizontallyCenter);
        GridLayout content = this.layout.addToContents(new GridLayout(0, 0));
        GridLayout.RowHelper gridHelper = content.columnSpacing(8).rowSpacing(4).createRowHelper(2);
        IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
        gridHelper.addChild(this.createGameRulesButton(singleplayerServer));
        gridHelper.addChild(this.difficultyButtons.layout());
        if (singleplayerServer != null) {
            gridHelper.addChild(this.createGameModeButton(singleplayerServer));
            gridHelper.addChild(this.createAllowCommandsButton(singleplayerServer));
        }
        gridHelper.addChild(this.createRestrictionsButton());
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
        WorldOptionsScreen worldOptionsScreen = this;
        this.layout.visitWidgets(x$0 -> worldOptionsScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    private CycleButton<GameType> createGameModeButton(IntegratedServer singleplayerServer) {
        this.gameModeButton = CycleButton.builder(GameType::getShortDisplayName, singleplayerServer.getWorldData().getGameType()).withValues((GameType[])new GameType[]{GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE, GameType.ADVENTURE}).create(GAME_MODE, (cycleButton, value) -> singleplayerServer.setWorldGameType((GameType)value));
        this.updateButton(this.gameModeButton, singleplayerServer, GAME_MODE_DISABLED_HARDCORE_TOOLTIP, GAME_MODE_DISABLED_OPERATOR_TOOLTIP);
        return this.gameModeButton;
    }

    private CycleButton<Boolean> createAllowCommandsButton(IntegratedServer singleplayerServer) {
        CycleButton<Boolean> allowCommandsButton = CycleButton.onOffBuilder(singleplayerServer.getWorldData().isAllowCommands()).create(ALLOW_COMMANDS, (cycleButton, value) -> singleplayerServer.setWorldAllowCommands((boolean)value));
        if (singleplayerServer.isHardcore() && (this.minecraft.player == null || !this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_OWNER))) {
            allowCommandsButton.active = false;
            allowCommandsButton.setTooltip(ALLOW_COMMANDS_DISABLED_TOOLTIP);
        }
        return allowCommandsButton;
    }

    private Button createGameRulesButton(@Nullable IntegratedServer singleplayerServer) {
        this.gameRulesButton = Button.builder(GAME_RULES, button -> {
            if (this.minecraft.player != null) {
                this.minecraft.gui.setScreen(new InWorldGameRulesScreen(this.minecraft.player.connection, optional -> this.minecraft.gui.setScreen(this), this));
            }
        }).build();
        this.updateButton(this.gameRulesButton, singleplayerServer, GAMERULES_DISABLED_HARDCORE_TOOLTIP, GAMERULES_DISABLED_TOOLTIP);
        return this.gameRulesButton;
    }

    private void updateButton(@Nullable AbstractWidget widget, @Nullable IntegratedServer singleplayerServer, Tooltip hardcoreTooltip, Tooltip disabledTooltip) {
        if (widget != null) {
            boolean hardcore = singleplayerServer != null && singleplayerServer.isHardcore();
            boolean hasGameMasterPermission = this.minecraft.player != null && this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
            boolean bl = widget.active = !hardcore && hasGameMasterPermission;
            widget.setTooltip((Tooltip)(hardcore ? hardcoreTooltip : (hasGameMasterPermission ? null : disabledTooltip)));
        }
    }

    private Button createRestrictionsButton() {
        return Button.builder(RESTRICTIONS, button -> {
            if (this.minecraft.player != null) {
                this.minecraft.gui.setScreen(new RestrictionsScreen(this, this.minecraft.player.chatAbilities()));
            }
        }).build();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @Override
    public void onGamemasterPermissionChanged(boolean hasGamemasterPermission) {
        IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
        this.updateButton(this.gameRulesButton, singleplayerServer, GAMERULES_DISABLED_HARDCORE_TOOLTIP, GAMERULES_DISABLED_TOOLTIP);
        this.updateButton(this.gameModeButton, singleplayerServer, GAME_MODE_DISABLED_HARDCORE_TOOLTIP, GAME_MODE_DISABLED_OPERATOR_TOOLTIP);
        if (!hasGamemasterPermission && !this.minecraft.hasSingleplayerServer()) {
            this.minecraft.gui.setScreen(this.lastScreen);
            Screen screen = this.minecraft.gui.screen();
            if (screen instanceof HasGamemasterPermissionReaction) {
                HasGamemasterPermissionReaction screen2 = (HasGamemasterPermissionReaction)((Object)screen);
                screen2.onGamemasterPermissionChanged(hasGamemasterPermission);
            }
        }
    }

    @Override
    public void added() {
        this.difficultyButtons.refresh(this.minecraft);
    }

    @Override
    public void onDifficultyChanged() {
        this.difficultyButtons.refresh(this.minecraft);
    }
}

