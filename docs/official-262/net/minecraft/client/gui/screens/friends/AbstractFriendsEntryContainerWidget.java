/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.friends;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;

@Environment(value=EnvType.CLIENT)
abstract class AbstractFriendsEntryContainerWidget
extends AbstractContainerWidget {
    static final int SPRITE_TEXTURE_SIZE = 18;
    static final int BUTTON_SIZE = 20;
    static final int FACE_SIZE = 24;
    static final int PADDING = 4;
    private static final int BACKGROUND_MARGIN = 4;
    protected final Minecraft minecraft;
    protected final FriendsOverlayScreen screen;
    protected final PlayerFaceWidget playerFaceWidget;
    protected final StringWidget nameWidget;
    protected final String playerName;
    protected final UUID playerId;
    protected final boolean showingStatus;
    private final List<AbstractWidget> children = new ArrayList<AbstractWidget>();

    public AbstractFriendsEntryContainerWidget(Minecraft minecraft, FriendsOverlayScreen screen, int x, int y, int width, int height, PlayerSocialManager.PlayerData playerData) {
        this(minecraft, screen, x, y, width, height, playerData, false);
    }

    public AbstractFriendsEntryContainerWidget(Minecraft minecraft, FriendsOverlayScreen screen, int x, int y, int width, int height, PlayerSocialManager.PlayerData playerData, boolean showingStatus) {
        super(x, y, width, height, Component.empty());
        this.minecraft = minecraft;
        this.screen = screen;
        this.playerName = playerData.name();
        this.playerId = playerData.id();
        this.playerFaceWidget = new PlayerFaceWidget(24, ResolvableProfile.createUnresolved(this.playerId));
        this.nameWidget = new StringWidget(Component.literal(this.playerName), minecraft.font);
        this.addChild(this.playerFaceWidget);
        this.addChild(this.nameWidget);
        this.showingStatus = showingStatus;
    }

    abstract void disable();

    UUID playerId() {
        return this.playerId;
    }

    protected abstract Component getEntryNarration();

    static Button.CreateNarration getSpriteIconNarration(Component actionDescription) {
        return supplier -> Component.translatable("narrator.select", actionDescription);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.nameWidget.getMessage());
        GuiEventListener focusedChild = this.getFocused();
        if (focusedChild instanceof AbstractWidget) {
            AbstractWidget focusedWidget = (AbstractWidget)focusedChild;
            focusedWidget.updateNarration(output.nest());
        } else {
            output.add(NarratedElementType.USAGE, this.getEntryNarration());
        }
    }

    @Override
    public Collection<? extends NarratableEntry> getNarratables() {
        ArrayList<AbstractWidget> narratables = new ArrayList<AbstractWidget>(this.children.size() + 1);
        narratables.addAll(this.children);
        narratables.add(this);
        return narratables;
    }

    protected final void addChild(AbstractWidget child) {
        this.children.add(child);
    }

    protected final void removeChild(AbstractWidget child) {
        if (this.children.remove(child) && this.getFocused() == child) {
            this.setFocused(null);
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    protected int contentHeight() {
        return this.height;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.minecraft.options.highContrast().get().booleanValue()) {
            graphics.fill(this.getX() - 4, this.getY(), this.getX() + this.getWidth() + 4, this.getY() + this.getHeight(), -16777216);
        }
        this.playerFaceWidget.setPosition(this.getX(), this.getY() + (this.getHeight() - this.playerFaceWidget.getHeight()) / 2);
        this.playerFaceWidget.extractRenderState(graphics, mouseX, mouseY, a);
        int nameY = this.getY() + this.getHeight() / (this.showingStatus ? 3 : 2) - this.nameWidget.getHeight() / 2;
        this.nameWidget.setPosition(this.playerFaceWidget.getRight() + 4, nameY);
        this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a);
    }
}

