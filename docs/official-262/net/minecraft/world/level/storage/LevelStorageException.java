/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage;

import net.minecraft.network.chat.Component;

public class LevelStorageException
extends RuntimeException {
    private final Component messageComponent;

    public LevelStorageException(Component message) {
        super(message.getString());
        this.messageComponent = message;
    }

    public LevelStorageException(Component message, Exception e) {
        super(message.getString(), e);
        this.messageComponent = message;
    }

    public Component getMessageComponent() {
        return this.messageComponent;
    }
}

