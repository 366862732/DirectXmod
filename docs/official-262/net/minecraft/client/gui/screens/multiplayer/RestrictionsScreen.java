/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.multiplayer;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatAbilities;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RestrictionsScreen
extends Screen {
    private static final Component TITLE = Component.translatable("restrictions_screen.title");
    private final Screen previousScreen;
    private final ChatAbilities chatAbilities;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private @Nullable ScrollableLayout bodyScroll;

    public RestrictionsScreen(Screen previousScreen, ChatAbilities chatAbilities) {
        super(TITLE);
        this.previousScreen = previousScreen;
        this.chatAbilities = chatAbilities;
    }

    @Override
    protected void init() {
        this.layout.addToHeader(new StringWidget(TITLE, this.font), LayoutSettings::alignHorizontallyCenter);
        LinearLayout body = LinearLayout.vertical();
        body.defaultCellSetting().alignHorizontallyCenter();
        int textBoxWidth = 250;
        this.chatAbilities.restrictions().forEach(restriction -> {
            body.addChild(FocusableTextWidget.builder(ComponentUtils.mergeStyles(restriction.display(), Style.EMPTY.withBold(true).withColor(ChatFormatting.RED)), this.font).maxWidth(250).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS).build().setCentered(true));
            restriction.action().ifPresent(action -> body.addChild(Button.builder(action.title(), button -> action.runnable().accept(this.minecraft, this.previousScreen)).width(200).build()));
        });
        if (this.chatAbilities.hasAnyRestrictions()) {
            body.addChild(new SpacerElement(10, 10));
        }
        ArrayList<Component> permissionEntries = new ArrayList<Component>();
        permissionEntries.add(RestrictionsScreen.createPermissionStatus(this.chatAbilities, Permissions.CHAT_SEND_MESSAGES, "send_messages"));
        permissionEntries.add(RestrictionsScreen.createPermissionStatus(this.chatAbilities, Permissions.CHAT_SEND_COMMANDS, "send_commands"));
        permissionEntries.add(RestrictionsScreen.createPermissionStatus(this.chatAbilities, Permissions.CHAT_RECEIVE_SYSTEM_MESSAGES, "receive_system_messages"));
        permissionEntries.add(RestrictionsScreen.createPermissionStatus(this.chatAbilities, Permissions.CHAT_RECEIVE_PLAYER_MESSAGES, "receive_player_messages"));
        Component message = CommonComponents.joinLines(permissionEntries);
        body.addChild(FocusableTextWidget.builder(message, this.font).maxWidth(250).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS).build().setCentered(true));
        this.bodyScroll = new ScrollableLayout(this.minecraft, body, this.layout.getContentHeight());
        this.layout.addToContents(this.bodyScroll);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
        RestrictionsScreen restrictionsScreen = this;
        this.layout.visitWidgets(x$0 -> restrictionsScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    private static Component createPermissionStatus(ChatAbilities abilities, Permission permission, String permissionId) {
        boolean isAllowed = abilities.permissions().hasPermission(permission);
        String prefix = "restrictions_screen.permission." + permissionId;
        if (isAllowed) {
            return Component.translatable(prefix + ".allowed").withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable(prefix + ".denied").withStyle(ChatFormatting.RED);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.previousScreen);
    }

    @Override
    protected void repositionElements() {
        this.bodyScroll.arrangeElements();
        this.bodyScroll.setMaxHeight(this.layout.getContentHeight());
        this.layout.arrangeElements();
    }
}

