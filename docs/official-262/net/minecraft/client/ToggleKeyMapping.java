/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;

@Environment(value=EnvType.CLIENT)
public class ToggleKeyMapping
extends KeyMapping {
    private final BooleanSupplier needsToggle;
    private boolean releasedByScreenWhenDown;
    private final boolean shouldRestore;

    public ToggleKeyMapping(String name, int value, KeyMapping.Category category, BooleanSupplier needsToggle, boolean shouldRestore) {
        this(name, InputConstants.Type.KEYSYM, value, category, needsToggle, shouldRestore);
    }

    public ToggleKeyMapping(String name, InputConstants.Type type, int value, KeyMapping.Category category, BooleanSupplier needsToggle, boolean shouldRestore) {
        super(name, type, value, category);
        this.needsToggle = needsToggle;
        this.shouldRestore = shouldRestore;
    }

    @Override
    protected boolean shouldSetOnIngameFocus() {
        return super.shouldSetOnIngameFocus() && !this.needsToggle.getAsBoolean();
    }

    @Override
    public void setDown(boolean down) {
        if (this.needsToggle.getAsBoolean()) {
            if (down) {
                super.setDown(!this.isDown());
            }
        } else {
            super.setDown(down);
        }
    }

    @Override
    protected void release() {
        if (this.needsToggle.getAsBoolean() && this.isDown() || this.releasedByScreenWhenDown) {
            this.releasedByScreenWhenDown = true;
        }
        this.reset();
    }

    public boolean shouldRestoreStateOnScreenClosed() {
        boolean shouldRestore = this.shouldRestore && this.needsToggle.getAsBoolean() && this.key.getType() == InputConstants.Type.KEYSYM && this.releasedByScreenWhenDown;
        this.releasedByScreenWhenDown = false;
        return shouldRestore;
    }

    protected void reset() {
        super.setDown(false);
    }
}

