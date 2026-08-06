/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;

public interface SimpleDialog
extends Dialog {
    public MapCodec<? extends SimpleDialog> codec();

    public List<ActionButton> mainActions();
}

