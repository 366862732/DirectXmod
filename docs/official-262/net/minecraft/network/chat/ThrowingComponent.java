/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.chat;

import net.minecraft.network.chat.Component;

public class ThrowingComponent
extends Exception {
    private final Component component;

    public ThrowingComponent(Component component) {
        super(component.getString());
        this.component = component;
    }

    public ThrowingComponent(Component component, Throwable cause) {
        super(component.getString(), cause);
        this.component = component;
    }

    public Component getComponent() {
        return this.component;
    }
}

