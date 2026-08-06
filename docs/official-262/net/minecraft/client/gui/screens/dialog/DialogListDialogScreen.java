/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.ButtonListDialogScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.action.StaticAction;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DialogListDialogScreen
extends ButtonListDialogScreen<DialogListDialog> {
    public DialogListDialogScreen(@Nullable Screen previousScreen, DialogListDialog dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    @Override
    protected Stream<ActionButton> createListActions(DialogListDialog data, DialogConnectionAccess connectionAccess) {
        return data.dialogs().stream().map(subDialog -> DialogListDialogScreen.createDialogClickAction(data, subDialog));
    }

    private static ActionButton createDialogClickAction(DialogListDialog data, Holder<Dialog> subDialog) {
        return new ActionButton(new CommonButtonData(subDialog.value().common().computeExternalTitle(), data.buttonWidth()), Optional.of(new StaticAction(new ClickEvent.ShowDialog(subDialog))));
    }
}

