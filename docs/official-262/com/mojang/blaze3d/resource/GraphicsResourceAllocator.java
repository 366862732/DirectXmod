/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.resource;

import com.mojang.blaze3d.resource.ResourceDescriptor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface GraphicsResourceAllocator {
    public static final GraphicsResourceAllocator UNPOOLED = new GraphicsResourceAllocator(){

        @Override
        public <T> T acquire(ResourceDescriptor<T> descriptor) {
            T resource = descriptor.allocate();
            descriptor.prepare(resource);
            return resource;
        }

        @Override
        public <T> void release(ResourceDescriptor<T> descriptor, T resource) {
            descriptor.free(resource);
        }
    };

    public <T> T acquire(ResourceDescriptor<T> var1);

    public <T> void release(ResourceDescriptor<T> var1, T var2);
}

