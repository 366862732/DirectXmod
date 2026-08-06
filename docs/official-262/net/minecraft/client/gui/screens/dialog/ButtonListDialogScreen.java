/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogControlSet;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.ButtonListDialog;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class ButtonListDialogScreen<T extends ButtonListDialog>
extends DialogScreen<T> {
    public static final int FOOTER_MARGIN = 5;

    public ButtonListDialogScreen(@Nullable Screen previousScreen, T dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    @Override
    protected void populateBodyElements(LinearLayout layout, DialogControlSet controlSet, T dialog, DialogConnectionAccess connectionAccess) {
        super.populateBodyElements(layout, controlSet, dialog, connectionAccess);
        List<Button> buttons = this.createListActions(dialog, connectionAccess).map(d -> controlSet.createActionButton((ActionButton)d).build()).toList();
        layout.addChild(ButtonListDialogScreen.packControlsIntoColumns(buttons, dialog.columns()));
    }

    protected abstract Stream<ActionButton> createListActions(T var1, DialogConnectionAccess var2);

    @Override
    protected void updateHeaderAndFooter(HeaderAndFooterLayout layout, DialogControlSet controlSet, T dialog, DialogConnectionAccess connectionAccess) {
        super.updateHeaderAndFooter(layout, controlSet, dialog, connectionAccess);
        dialog.exitAction().ifPresentOrElse(exitButton -> layout.addToFooter(controlSet.createActionButton((ActionButton)exitButton).build()), () -> layout.setFooterHeight(5));
    }
}

