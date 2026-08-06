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
import java.util.List;
import java.util.Optional;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.SimpleDialog;
import net.minecraft.server.dialog.action.Action;

public record ConfirmationDialog(CommonDialogData common, ActionButton yesButton, ActionButton noButton) implements SimpleDialog
{
    public static final MapCodec<ConfirmationDialog> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CommonDialogData.MAP_CODEC.forGetter(ConfirmationDialog::common), (App)ActionButton.CODEC.fieldOf("yes").forGetter(ConfirmationDialog::yesButton), (App)ActionButton.CODEC.fieldOf("no").forGetter(ConfirmationDialog::noButton)).apply((Applicative)i, ConfirmationDialog::new));

    public MapCodec<ConfirmationDialog> codec() {
        return MAP_CODEC;
    }

    @Override
    public Optional<Action> onCancel() {
        return this.noButton.action();
    }

    @Override
    public List<ActionButton> mainActions() {
        return List.of(this.yesButton, this.noButton);
    }
}

