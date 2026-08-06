/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.packs.resources;

import net.minecraft.server.packs.resources.ResourceManager;

public interface CloseableResourceManager
extends ResourceManager,
AutoCloseable {
    @Override
    public void close();
}

