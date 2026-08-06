/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.action.Action;

public interface ButtonListDialog
extends Dialog {
    public MapCodec<? extends ButtonListDialog> codec();

    public int columns();

    public Optional<ActionButton> exitAction();

    @Override
    default public Optional<Action> onCancel() {
        return this.exitAction().flatMap(ActionButton::action);
    }
}

