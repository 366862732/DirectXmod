/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.dialog.body;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;

public record ItemBody(ItemStackTemplate item, Optional<PlainMessage> description, boolean showDecorations, boolean showTooltip, int width, int height) implements DialogBody
{
    public static final MapCodec<ItemBody> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ItemStackTemplate.CODEC.fieldOf("item").forGetter(ItemBody::item), (App)PlainMessage.CODEC.optionalFieldOf("description").forGetter(ItemBody::description), (App)Codec.BOOL.optionalFieldOf("show_decorations", (Object)true).forGetter(ItemBody::showDecorations), (App)Codec.BOOL.optionalFieldOf("show_tooltip", (Object)true).forGetter(ItemBody::showTooltip), (App)ExtraCodecs.intRange(1, 256).optionalFieldOf("width", (Object)16).forGetter(ItemBody::width), (App)ExtraCodecs.intRange(1, 256).optionalFieldOf("height", (Object)16).forGetter(ItemBody::height)).apply((Applicative)i, ItemBody::new));

    public MapCodec<ItemBody> mapCodec() {
        return MAP_CODEC;
    }
}

