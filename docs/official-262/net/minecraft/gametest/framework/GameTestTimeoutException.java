/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import net.minecraft.gametest.framework.GameTestException;
import net.minecraft.network.chat.Component;

public class GameTestTimeoutException
extends GameTestException {
    protected final Component message;

    public GameTestTimeoutException(Component message) {
        super(message.getString());
        this.message = message;
    }

    @Override
    public Component getDescription() {
        return this.message;
    }
}

