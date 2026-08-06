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
import net.minecraft.server.dialog.ButtonListDialog;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.util.ExtraCodecs;

public record MultiActionDialog(CommonDialogData common, List<ActionButton> actions, Optional<ActionButton> exitAction, int columns) implements ButtonListDialog
{
    public static final MapCodec<MultiActionDialog> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CommonDialogData.MAP_CODEC.forGetter(MultiActionDialog::common), (App)ExtraCodecs.nonEmptyList(ActionButton.CODEC.listOf()).fieldOf("actions").forGetter(MultiActionDialog::actions), (App)ActionButton.CODEC.optionalFieldOf("exit_action").forGetter(MultiActionDialog::exitAction), (App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("columns", (Object)2).forGetter(MultiActionDialog::columns)).apply((Applicative)i, MultiActionDialog::new));

    public MapCodec<MultiActionDialog> codec() {
        return MAP_CODEC;
    }
}

