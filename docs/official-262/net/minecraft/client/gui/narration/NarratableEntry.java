/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.narration;

import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.narration.NarrationSupplier;

@Environment(value=EnvType.CLIENT)
public interface NarratableEntry
extends NarrationSupplier,
TabOrderedElement {
    public NarrationPriority narrationPriority();

    default public boolean isActive() {
        return true;
    }

    default public Collection<? extends NarratableEntry> getNarratables() {
        return List.of(this);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum NarrationPriority {
        NONE,
        HOVERED,
        FOCUSED;


        public boolean isTerminal() {
            return this == FOCUSED;
        }
    }
}

