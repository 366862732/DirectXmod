/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ReferenceArrayList
 *  it.unimi.dsi.fastutil.objects.ReferenceList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.vulkan;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class DestructionQueue<T>
implements AutoCloseable {
    private final Destroyer<T> destroyCallback;
    private final ReferenceList<ReferenceArrayList<T>> destructionQueues;
    private int currentDestructionQueueIndex = 0;

    public DestructionQueue(int internalQueueCount, Destroyer<T> destroyCallback) {
        this.destroyCallback = destroyCallback;
        this.destructionQueues = new ReferenceArrayList(internalQueueCount);
        for (int i = 0; i < internalQueueCount; ++i) {
            this.destructionQueues.add((Object)new ReferenceArrayList());
        }
    }

    @Override
    public void close() {
        for (int i = 0; i < this.destructionQueues.size(); ++i) {
            if (!this.rotate()) continue;
            i = 0;
        }
    }

    public boolean rotate() {
        ++this.currentDestructionQueueIndex;
        this.currentDestructionQueueIndex %= this.destructionQueues.size();
        ReferenceArrayList currentQueue = (ReferenceArrayList)this.destructionQueues.set(this.currentDestructionQueueIndex, (Object)new ReferenceArrayList());
        if (currentQueue.isEmpty()) {
            return false;
        }
        this.destroyCallback.begin(currentQueue.size());
        currentQueue.forEach(this.destroyCallback::destroy);
        this.destroyCallback.end();
        return true;
    }

    public void add(T t) {
        ReferenceArrayList currentQueue = (ReferenceArrayList)this.destructionQueues.get(this.currentDestructionQueueIndex);
        currentQueue.add(t);
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Destroyer<T> {
        default public void begin(int count) {
        }

        public void destroy(T var1);

        default public void end() {
        }
    }
}

