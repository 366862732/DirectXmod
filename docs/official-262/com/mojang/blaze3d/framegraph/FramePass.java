/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.framegraph;

import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface FramePass {
    public <T> ResourceHandle<T> createsInternal(String var1, ResourceDescriptor<T> var2);

    public <T> void reads(ResourceHandle<T> var1);

    public <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> var1);

    public void requires(FramePass var1);

    public void disableCulling();

    public void executes(Runnable var1);
}

