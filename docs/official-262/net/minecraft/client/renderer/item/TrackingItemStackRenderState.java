/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.item;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ItemStackRenderState;

@Environment(value=EnvType.CLIENT)
public class TrackingItemStackRenderState
extends ItemStackRenderState {
    private final List<Object> modelIdentityElements = new ArrayList<Object>();

    @Override
    public void appendModelIdentityElement(Object element) {
        this.modelIdentityElements.add(element);
    }

    public Object getModelIdentity() {
        return this.modelIdentityElements;
    }
}

