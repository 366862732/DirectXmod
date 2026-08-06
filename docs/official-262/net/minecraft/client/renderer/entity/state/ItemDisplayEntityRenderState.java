/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

@Environment(value=EnvType.CLIENT)
public class ItemDisplayEntityRenderState
extends DisplayEntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();

    @Override
    public boolean hasSubState() {
        return !this.item.isEmpty();
    }
}

