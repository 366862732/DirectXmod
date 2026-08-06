/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.dialog;

import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogControlSet;
import net.minecraft.client.gui.screens.dialog.WaitingForResponseScreen;
import net.minecraft.client.gui.screens.dialog.body.DialogBodyHandlers;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.body.DialogBody;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class DialogScreen<T extends Dialog>
extends Screen {
    public static final Component DISCONNECT = Component.translatable("menu.custom_screen_info.disconnect");
    private static final int WARNING_BUTTON_SIZE = 20;
    private static final WidgetSprites WARNING_BUTTON_SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("dialog/warning_button"), Identifier.withDefaultNamespace("dialog/warning_button_disabled"), Identifier.withDefaultNamespace("dialog/warning_button_highlighted"));
    private final T dialog;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final @Nullable Screen previousScreen;
    private @Nullable ScrollableLayout bodyScroll;
    private Button warningButton;
    private final DialogConnectionAccess connectionAccess;
    private Supplier<Optional<ClickEvent>> onClose = DialogControlSet.EMPTY_ACTION;

    public DialogScreen(@Nullable Screen previousScreen, T dialog, DialogConnectionAccess connectionAccess) {
        super(dialog.common().title());
        this.dialog = dialog;
        this.previousScreen = previousScreen;
        this.connectionAccess = connectionAccess;
    }

    @Override
    protected final void init() {
        super.init();
        this.warningButton = this.createWarningButton();
        this.warningButton.setTabOrderGroup(-10);
        DialogControlSet controlSet = new DialogControlSet(this);
        LinearLayout body = LinearLayout.vertical().spacing(10);
        body.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addToHeader(this.createTitleWithWarningButton());
        for (DialogBody dialogBody : this.dialog.common().body()) {
            LayoutElement bodyElement = DialogBodyHandlers.createBodyElement(this, dialogBody);
            if (bodyElement == null) continue;
            body.addChild(bodyElement);
        }
        for (Input input : this.dialog.common().inputs()) {
            controlSet.addInput(input, body::addChild);
        }
        this.populateBodyElements(body, controlSet, this.dialog, this.connectionAccess);
        this.bodyScroll = new ScrollableLayout(this.minecraft, body, this.layout.getContentHeight());
        this.layout.addToContents(this.bodyScroll);
        this.updateHeaderAndFooter(this.layout, controlSet, this.dialog, this.connectionAccess);
        this.onClose = controlSet.bindAction(this.dialog.onCancel());
        this.layout.visitWidgets(widget -> {
            if (widget != this.warningButton) {
                this.addRenderableWidget(widget);
            }
        });
        this.addRenderableWidget(this.warningButton);
        this.repositionElements();
    }

    protected void populateBodyElements(LinearLayout layout, DialogControlSet controlSet, T dialog, DialogConnectionAccess connectionAccess) {
    }

    protected void updateHeaderAndFooter(HeaderAndFooterLayout layout, DialogControlSet controlSet, T dialog, DialogConnectionAccess connectionAccess) {
    }

    @Override
    protected void repositionElements() {
        this.bodyScroll.arrangeElements();
        this.bodyScroll.setMaxHeight(this.layout.getContentHeight());
        this.layout.arrangeElements();
        this.makeSureWarningButtonIsInBounds();
    }

    protected LayoutElement createTitleWithWarningButton() {
        LinearLayout layout = LinearLayout.horizontal().spacing(10);
        layout.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        layout.addChild(new StringWidget(this.title, this.font));
        layout.addChild(this.warningButton);
        return layout;
    }

    protected void makeSureWarningButtonIsInBounds() {
        int x = this.warningButton.getX();
        int y = this.warningButton.getY();
        if (x < 0 || y < 0 || x > this.width - 20 || y > this.height - 20) {
            this.warningButton.setX(Math.max(0, this.width - 40));
            this.warningButton.setY(Math.min(5, this.height));
        }
    }

    private Button createWarningButton() {
        ImageButton result = new ImageButton(0, 0, 20, 20, WARNING_BUTTON_SPRITES, button -> this.minecraft.gui.setScreen(WarningScreen.create(this.minecraft, this.connectionAccess, this)), Component.translatable("menu.custom_screen_info.button_narration"));
        result.setTooltip(Tooltip.create(Component.translatable("menu.custom_screen_info.tooltip")));
        return result;
    }

    @Override
    public boolean isPauseScreen() {
        return this.dialog.common().pause();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.dialog.common().canCloseWithEscape();
    }

    @Override
    public void onClose() {
        this.runAction(this.onClose.get(), DialogAction.CLOSE);
    }

    public void runAction(Optional<ClickEvent> closeAction) {
        this.runAction(closeAction, this.dialog.common().afterAction());
    }

    public void runAction(Optional<ClickEvent> closeAction, DialogAction afterAction) {
        Screen screenToActivate;
        switch (afterAction) {
            default: {
                throw new MatchException(null, null);
            }
            case NONE: {
                Screen screen = this;
                break;
            }
            case CLOSE: {
                Screen screen = this.previousScreen;
                break;
            }
            case WAIT_FOR_RESPONSE: {
                Screen screen = screenToActivate = new WaitingForResponseScreen(this.previousScreen);
            }
        }
        if (closeAction.isPresent()) {
            this.handleDialogClickEvent(closeAction.get(), screenToActivate);
        } else {
            this.minecraft.gui.setScreen(screenToActivate);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void handleDialogClickEvent(ClickEvent event, @Nullable Screen activeScreen) {
        ClickEvent clickEvent = event;
        Objects.requireNonNull(clickEvent);
        ClickEvent clickEvent2 = clickEvent;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ClickEvent.RunCommand.class, ClickEvent.ShowDialog.class, ClickEvent.Custom.class}, (ClickEvent)clickEvent2, n)) {
            case 0: {
                ClickEvent.RunCommand runCommand = (ClickEvent.RunCommand)clickEvent2;
                try {
                    String string;
                    String command = string = runCommand.command();
                    this.connectionAccess.runCommand(Commands.trimOptionalPrefix(command), activeScreen);
                    return;
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
            }
            case 1: {
                ClickEvent.ShowDialog dialog = (ClickEvent.ShowDialog)clickEvent2;
                this.connectionAccess.openDialog(dialog.dialog(), activeScreen);
                return;
            }
            case 2: {
                ClickEvent.Custom custom = (ClickEvent.Custom)clickEvent2;
                this.connectionAccess.sendCustomAction(custom.id(), custom.payload());
                this.minecraft.gui.setScreen(activeScreen);
                return;
            }
        }
        DialogScreen.defaultHandleClickEvent(event, this.minecraft, activeScreen);
    }

    public @Nullable Screen previousScreen() {
        return this.previousScreen;
    }

    protected static LayoutElement packControlsIntoColumns(List<? extends LayoutElement> controls, int columns) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().alignHorizontallyCenter();
        gridLayout.columnSpacing(2).rowSpacing(2);
        int count = controls.size();
        int lastFullRow = count / columns;
        int countInFullRows = lastFullRow * columns;
        for (int i = 0; i < countInFullRows; ++i) {
            gridLayout.addChild(controls.get(i), i / columns, i % columns);
        }
        if (count != countInFullRows) {
            LinearLayout lastRow = LinearLayout.horizontal().spacing(2);
            lastRow.defaultCellSetting().alignHorizontallyCenter();
            for (int i = countInFullRows; i < count; ++i) {
                lastRow.addChild(controls.get(i));
            }
            gridLayout.addChild(lastRow, lastFullRow, 0, 1, columns);
        }
        return gridLayout;
    }

    @Environment(value=EnvType.CLIENT)
    public static class WarningScreen
    extends ConfirmScreen {
        private final MutableObject<@Nullable Screen> returnScreen;

        public static Screen create(Minecraft minecraft, DialogConnectionAccess connectionAccess, Screen returnScreen) {
            return new WarningScreen(minecraft, connectionAccess, (MutableObject<Screen>)new MutableObject((Object)returnScreen));
        }

        private WarningScreen(Minecraft minecraft, DialogConnectionAccess connectionAccess, MutableObject<Screen> returnScreen) {
            super(disconnect -> {
                if (disconnect) {
                    connectionAccess.disconnect(DISCONNECT);
                } else {
                    minecraft.gui.setScreen((Screen)returnScreen.get());
                }
            }, Component.translatable("menu.custom_screen_info.title"), Component.translatable("menu.custom_screen_info.contents"), CommonComponents.disconnectButtonLabel(minecraft.isLocalServer()), CommonComponents.GUI_BACK);
            this.returnScreen = returnScreen;
        }

        public @Nullable Screen returnScreen() {
            return (Screen)this.returnScreen.get();
        }

        public void updateReturnScreen(@Nullable Screen newReturnScreen) {
            this.returnScreen.setValue((Object)newReturnScreen);
        }
    }
}

