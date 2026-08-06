/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.GLX;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugMemoryUntracker {
    private static final @Nullable MethodHandle UNTRACK = GLX.make(() -> {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> debugAllocator = Class.forName("org.lwjgl.system.MemoryManage$DebugAllocator");
            Method reflectionUntrack = debugAllocator.getDeclaredMethod("untrack", Long.TYPE);
            reflectionUntrack.setAccessible(true);
            Field allocatorField = Class.forName("org.lwjgl.system.MemoryUtil$LazyInit").getDeclaredField("ALLOCATOR");
            allocatorField.setAccessible(true);
            Object allocator = allocatorField.get(null);
            if (debugAllocator.isInstance(allocator)) {
                return lookup.unreflect(reflectionUntrack);
            }
            return null;
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    });

    public static void untrack(long address) {
        if (UNTRACK == null) {
            return;
        }
        try {
            UNTRACK.invoke(address);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}

