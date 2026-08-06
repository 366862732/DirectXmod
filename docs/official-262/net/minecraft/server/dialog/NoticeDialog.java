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
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.SimpleDialog;
import net.minecraft.server.dialog.action.Action;

public record NoticeDialog(CommonDialogData common, ActionButton action) implements SimpleDialog
{
    public static final ActionButton DEFAULT_ACTION = new ActionButton(new CommonButtonData(CommonComponents.GUI_OK, 150), Optional.empty());
    public static final MapCodec<NoticeDialog> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CommonDialogData.MAP_CODEC.forGetter(NoticeDialog::common), (App)ActionButton.CODEC.optionalFieldOf("action", (Object)DEFAULT_ACTION).forGetter(NoticeDialog::action)).apply((Applicative)i, NoticeDialog::new));

    public MapCodec<NoticeDialog> codec() {
        return MAP_CODEC;
    }

    @Override
    public Optional<Action> onCancel() {
        return this.action.action();
    }

    @Override
    public List<ActionButton> mainActions() {
        return List.of(this.action);
    }
}

