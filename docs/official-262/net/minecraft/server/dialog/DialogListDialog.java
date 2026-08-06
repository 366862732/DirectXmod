/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.dialog;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.ButtonListDialog;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;

public record DialogListDialog(CommonDialogData common, HolderSet<Dialog> dialogs, Optional<ActionButton> exitAction, int columns, int buttonWidth) implements ButtonListDialog
{
    public static final MapCodec<DialogListDialog> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CommonDialogData.MAP_CODEC.forGetter(DialogListDialog::common), (App)Dialog.LIST_CODEC.fieldOf("dialogs").forGetter(DialogListDialog::dialogs), (App)ActionButton.CODEC.optionalFieldOf("exit_action").forGetter(DialogListDialog::exitAction), (App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("columns", (Object)2).forGetter(DialogListDialog::columns), (App)WIDTH_CODEC.optionalFieldOf("button_width", (Object)150).forGetter(DialogListDialog::buttonWidth)).apply((Applicative)i, DialogListDialog::new));

    public MapCodec<DialogListDialog> codec() {
        return MAP_CODEC;
    }
}

