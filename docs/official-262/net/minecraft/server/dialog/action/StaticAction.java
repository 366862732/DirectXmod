/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.util.Util;

public record StaticAction(ClickEvent value) implements Action
{
    public static final Map<ClickEvent.Action, MapCodec<StaticAction>> WRAPPED_CODECS = Util.make(() -> {
        EnumMap<ClickEvent.Action, MapCodec> result = new EnumMap<ClickEvent.Action, MapCodec>(ClickEvent.Action.class);
        for (ClickEvent.Action action : (ClickEvent.Action[])ClickEvent.Action.class.getEnumConstants()) {
            if (!action.isAllowedFromServer()) continue;
            MapCodec<? extends ClickEvent> mapCodec = action.valueCodec();
            result.put(action, mapCodec.xmap(StaticAction::new, StaticAction::value));
        }
        return Collections.unmodifiableMap(result);
    });

    public MapCodec<StaticAction> codec() {
        return WRAPPED_CODECS.get(this.value.action());
    }

    @Override
    public Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> parameters) {
        return Optional.of(this.value);
    }
}

