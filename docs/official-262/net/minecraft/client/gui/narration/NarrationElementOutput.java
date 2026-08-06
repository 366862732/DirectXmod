/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.narration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationThunk;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public interface NarrationElementOutput {
    default public void add(NarratedElementType type, Component contents) {
        this.add(type, NarrationThunk.from(contents.getString()));
    }

    default public void add(NarratedElementType type, String contents) {
        this.add(type, NarrationThunk.from(contents));
    }

    default public void add(NarratedElementType type, Component ... contents) {
        this.add(type, NarrationThunk.from((List<Component>)ImmutableList.copyOf((Object[])contents)));
    }

    public void add(NarratedElementType var1, NarrationThunk<?> var2);

    public NarrationElementOutput nest();
}

