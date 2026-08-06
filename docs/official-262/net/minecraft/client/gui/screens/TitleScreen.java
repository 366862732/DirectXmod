/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.BanDetails
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import java.io.IOException;
import java.lang.invoke.LambdaMetafactory;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Panorama;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TitleScreen
extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("narrator.screen.title");
    private static final Component COPYRIGHT_TEXT = Component.translatable("title.credits");
    private static final String DEMO_LEVEL_ID = "Demo_World";
    private @Nullable SplashRenderer splash;
    private @Nullable RealmsNotificationsScreen realmsNotificationsScreen;
    private @Nullable FriendsButton friends;
    private boolean fading;
    private long fadeInStart;
    private final LogoRenderer logoRenderer;

    public TitleScreen() {
        this(false);
    }

    public TitleScreen(boolean fading) {
        this(fading, null);
    }

    public TitleScreen(boolean fading, @Nullable LogoRenderer logoRenderer) {
        super(TITLE);
        this.fading = fading;
        this.logoRenderer = Objects.requireNonNullElseGet(logoRenderer, () -> new LogoRenderer(false));
        this.minecraft.gameRenderer.panorama().startSpin();
    }

    private boolean realmsNotificationsEnabled() {
        return this.realmsNotificationsScreen != null;
    }

    @Override
    public void tick() {
        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.tick();
        }
        if (this.minecraft.getPlayerSocialManager().isFriendListEnabled() && this.friends != null) {
            this.friends.refreshIncomingRequestCount();
        }
    }

    public static void registerTextures(TextureManager textureManager) {
        textureManager.registerForNextReload(LogoRenderer.MINECRAFT_LOGO);
        textureManager.registerForNextReload(LogoRenderer.MINECRAFT_EDITION);
        textureManager.registerForNextReload(Panorama.PANORAMA_OVERLAY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        if (this.splash == null) {
            this.splash = this.minecraft.gui.splashManager().getSplash();
        }
        int copyrightWidth = this.font.width(COPYRIGHT_TEXT);
        int copyrightX = this.width - copyrightWidth - 2;
        int spacing = 24;
        int topPos = this.height / 4 + 48;
        topPos = this.minecraft.isDemo() ? this.createDemoMenuOptions(topPos, 24) : this.createNormalMenuOptions(topPos, 24);
        int numberOfButtons = 3;
        int currentButton = 0;
        this.friends = this.addRenderableWidget(CommonButtons.friends(20, button -> OnlineOptionsScreen.confirmFriendsListEnabled(this.minecraft, () -> this.minecraft.gui.setScreen(new FriendsOverlayScreen(this)), this), !this.minecraft.isDemo()));
        this.friends.setPosition(this.getHorizontalPosition(++currentButton, 3, 20), topPos += 24);
        SpriteIconButton language = this.addRenderableWidget(CommonButtons.language(20, button -> this.minecraft.gui.setScreen(new LanguageSelectScreen((Screen)this, this.minecraft.options, this.minecraft.getLanguageManager())), true));
        language.setPosition(this.getHorizontalPosition(++currentButton, 3, 20), topPos);
        SpriteIconButton accessibility = this.addRenderableWidget(CommonButtons.accessibility(20, button -> this.minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), true));
        accessibility.setPosition(this.getHorizontalPosition(++currentButton, 3, 20), topPos);
        this.addRenderableWidget(Button.builder(Component.translatable("menu.options"), button -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options, false))).bounds(this.width / 2 - 100, topPos += 24, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), button -> this.minecraft.stop()).bounds(this.width / 2 + 2, topPos, 98, 20).build());
        this.addRenderableWidget(new PlainTextButton(copyrightX, this.height - 10, copyrightWidth, 10, COPYRIGHT_TEXT, button -> this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this)), this.font));
        if (this.realmsNotificationsScreen == null) {
            this.realmsNotificationsScreen = new RealmsNotificationsScreen();
        }
        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.init(this.width, this.height);
        }
    }

    private int getHorizontalPosition(int currentButton, int numberOfButtons, int buttonWidth) {
        int totalWidth = numberOfButtons * buttonWidth + (numberOfButtons - 1) * 4;
        return this.width / 2 - totalWidth / 2 + (currentButton - 1) * (buttonWidth + 4);
    }

    private int createNormalMenuOptions(int topPos, int spacing) {
        Component multiplayerDisabledReason;
        Button singleplayerButton = this.addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"), button -> this.minecraft.gui.setScreen(new SelectWorldScreen(this))).bounds(this.width / 2 - 100, topPos, 200, 20).build());
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            this.addRenderableWidget(Button.builder(Component.literal("TW"), button -> CreateWorldScreen.testWorld(this.minecraft, () -> this.minecraft.gui.setScreen(this))).bounds(singleplayerButton.getX() + singleplayerButton.getWidth() + 2, topPos, 20, 20).build());
        }
        boolean multiplayerAllowed = (multiplayerDisabledReason = this.getMultiplayerDisabledReason()) == null;
        Tooltip tooltip = multiplayerDisabledReason != null ? Tooltip.create(multiplayerDisabledReason) : null;
        topPos += spacing;
        this.addRenderableWidget(Button.builder((Component)Component.translatable((String)"menu.multiplayer"), (Button.OnPress)(Button.OnPress)LambdaMetafactory.metafactory(null, null, null, (Lnet/minecraft/client/gui/components/Button;)V, lambda$createNormalMenuOptions$3(net.minecraft.client.gui.components.Button ), (Lnet/minecraft/client/gui/components/Button;)V)((TitleScreen)this)).bounds((int)(this.width / 2 - 100), (int)v0, (int)200, (int)20).tooltip((Tooltip)tooltip).build()).active = multiplayerAllowed;
        this.addRenderableWidget(Button.builder((Component)Component.translatable((String)"menu.online"), (Button.OnPress)(Button.OnPress)LambdaMetafactory.metafactory(null, null, null, (Lnet/minecraft/client/gui/components/Button;)V, lambda$createNormalMenuOptions$4(net.minecraft.client.gui.components.Button ), (Lnet/minecraft/client/gui/components/Button;)V)((TitleScreen)this)).bounds((int)(this.width / 2 - 100), (int)v1, (int)200, (int)20).tooltip((Tooltip)tooltip).build()).active = multiplayerAllowed;
        return topPos += spacing;
    }

    private @Nullable Component getMultiplayerDisabledReason() {
        if (this.minecraft.allowsMultiplayer()) {
            return null;
        }
        if (this.minecraft.isNameBanned()) {
            return Component.translatable("title.multiplayer.disabled.banned.name");
        }
        BanDetails multiplayerBan = this.minecraft.multiplayerBan();
        if (multiplayerBan != null) {
            if (multiplayerBan.expires() != null) {
                return Component.translatable("title.multiplayer.disabled.banned.temporary");
            }
            return Component.translatable("title.multiplayer.disabled.banned.permanent");
        }
        return Component.translatable("title.multiplayer.disabled");
    }

    private int createDemoMenuOptions(int topPos, int spacing) {
        boolean demoWorldPresent = this.checkDemoWorldPresence();
        this.addRenderableWidget(Button.builder(Component.translatable("menu.playdemo"), button -> {
            if (demoWorldPresent) {
                this.minecraft.createWorldOpenFlows().openWorld(DEMO_LEVEL_ID, () -> this.minecraft.gui.setScreen(this));
            } else {
                this.minecraft.createWorldOpenFlows().createFreshLevel(DEMO_LEVEL_ID, MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
            }
        }).bounds(this.width / 2 - 100, topPos, 200, 20).build());
        Button resetDemoButton = this.addRenderableWidget(Button.builder(Component.translatable("menu.resetdemo"), button -> {
            LevelStorageSource levelSource = this.minecraft.getLevelSource();
            try (LevelStorageSource.LevelStorageAccess levelAccess = levelSource.createAccess(DEMO_LEVEL_ID);){
                if (levelAccess.hasWorldData()) {
                    this.minecraft.gui.setScreen(new ConfirmScreen(this::confirmDemo, Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", MinecraftServer.DEMO_SETTINGS.levelName()), Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
                }
            }
            catch (IOException e) {
                SystemToast.onWorldAccessFailure(this.minecraft, DEMO_LEVEL_ID);
                LOGGER.warn("Failed to access demo world", (Throwable)e);
            }
        }).bounds(this.width / 2 - 100, topPos += spacing, 200, 20).build());
        resetDemoButton.active = demoWorldPresent;
        return topPos;
    }

    private boolean checkDemoWorldPresence() {
        boolean bl;
        block8: {
            LevelStorageSource.LevelStorageAccess levelSource = this.minecraft.getLevelSource().createAccess(DEMO_LEVEL_ID);
            try {
                bl = levelSource.hasWorldData();
                if (levelSource == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (levelSource != null) {
                        try {
                            levelSource.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    SystemToast.onWorldAccessFailure(this.minecraft, DEMO_LEVEL_ID);
                    LOGGER.warn("Failed to read demo world data", (Throwable)e);
                    return false;
                }
            }
            levelSource.close();
        }
        return bl;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }
        float widgetFade = 1.0f;
        if (this.fading) {
            float fade = (float)(Util.getMillis() - this.fadeInStart) / 2000.0f;
            if (fade > 1.0f) {
                this.fading = false;
            } else {
                fade = Mth.clamp(fade, 0.0f, 1.0f);
                widgetFade = Mth.clampedMap(fade, 0.5f, 1.0f, 0.0f, 1.0f);
            }
            this.fadeWidgets(widgetFade);
        }
        this.extractPanorama(graphics, a);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.logoRenderer.extractRenderState(graphics, this.width, this.logoRenderer.keepLogoThroughFade() ? 1.0f : widgetFade);
        if (this.splash != null && !this.minecraft.options.hideSplashTexts().get().booleanValue()) {
            this.splash.extractRenderState(graphics, this.width, this.font, widgetFade);
        }
        String versionString = "Minecraft " + SharedConstants.getCurrentVersion().name();
        if (this.minecraft.isDemo()) {
            versionString = versionString + " Demo";
        }
        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            versionString = versionString + I18n.get("menu.modded", new Object[0]);
        }
        graphics.text(this.font, versionString, 2, this.height - 10, ARGB.white(widgetFade));
        if (this.realmsNotificationsEnabled() && widgetFade >= 1.0f) {
            this.realmsNotificationsScreen.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        return this.realmsNotificationsEnabled() && this.realmsNotificationsScreen.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.removed();
        }
    }

    @Override
    public void added() {
        super.added();
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.added();
        }
    }

    private void confirmDemo(boolean result) {
        if (result) {
            try (LevelStorageSource.LevelStorageAccess levelSource = this.minecraft.getLevelSource().createAccess(DEMO_LEVEL_ID);){
                levelSource.deleteLevel();
            }
            catch (IOException e) {
                SystemToast.onWorldDeleteFailure(this.minecraft, DEMO_LEVEL_ID);
                LOGGER.warn("Failed to delete demo world", (Throwable)e);
            }
        }
        this.minecraft.gui.setScreen(this);
    }

    @Override
    public boolean canInterruptWithAnotherScreen() {
        return true;
    }

    private /* synthetic */ void lambda$createNormalMenuOptions$4(Button button) {
        this.minecraft.gui.setScreen(new RealmsMainScreen(this));
    }

    private /* synthetic */ void lambda$createNormalMenuOptions$3(Button button) {
        Screen screen = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this);
        this.minecraft.gui.setScreen(screen);
    }
}

