/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.dialog.action;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.action.Action;

public record CustomAll(Identifier id, Optional<CompoundTag> additions) implements Action
{
    public static final MapCodec<CustomAll> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("id").forGetter(CustomAll::id), (App)CompoundTag.CODEC.optionalFieldOf("additions").forGetter(CustomAll::additions)).apply((Applicative)i, CustomAll::new));

    public MapCodec<CustomAll> codec() {
        return MAP_CODEC;
    }

    @Override
    public Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> parameters) {
        CompoundTag tag = this.additions.map(CompoundTag::copy).orElseGet(CompoundTag::new);
        parameters.forEach((key, value) -> tag.put((String)key, value.asTag()));
        return Optional.of(new ClickEvent.Custom(this.id, Optional.of(tag)));
    }
}

