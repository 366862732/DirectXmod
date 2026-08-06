/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogControlSet;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.SimpleDialog;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SimpleDialogScreen<T extends SimpleDialog>
extends DialogScreen<T> {
    public SimpleDialogScreen(@Nullable Screen previousScreen, T dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    @Override
    protected void updateHeaderAndFooter(HeaderAndFooterLayout layout, DialogControlSet controlSet, T dialog, DialogConnectionAccess connectionAccess) {
        super.updateHeaderAndFooter(layout, controlSet, dialog, connectionAccess);
        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(8);
        for (ActionButton action : dialog.mainActions()) {
            buttonLayout.addChild(controlSet.createActionButton(action).build());
        }
        layout.addToFooter(buttonLayout);
    }
}

