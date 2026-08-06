/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.server.dialog.body;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;

public class DialogBodyTypes {
    public static MapCodec<? extends DialogBody> bootstrap(Registry<MapCodec<? extends DialogBody>> registry) {
        Registry.register(registry, Identifier.withDefaultNamespace("item"), ItemBody.MAP_CODEC);
        return Registry.register(registry, Identifier.withDefaultNamespace("plain_message"), PlainMessage.MAP_CODEC);
    }
}

