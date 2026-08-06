/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.glfw.GLFWErrorCallbackI
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public class GLFWErrorCapture
implements GLFWErrorCallbackI,
Iterable<Error> {
    private @Nullable List<Error> errors;

    public void invoke(int error, long description) {
        if (this.errors == null) {
            this.errors = new ArrayList<Error>();
        }
        this.errors.add(new Error(error, MemoryUtil.memUTF8((long)description)));
    }

    @Override
    public Iterator<Error> iterator() {
        return this.errors == null ? Collections.emptyIterator() : this.errors.iterator();
    }

    public @Nullable Error firstError() {
        return this.errors == null ? null : this.errors.getFirst();
    }

    @Environment(value=EnvType.CLIENT)
    public record Error(int error, String description) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "[GLFW 0x%X] %s", this.error, this.description);
        }
    }
}

