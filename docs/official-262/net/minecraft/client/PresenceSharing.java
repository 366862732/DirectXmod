/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

@Environment(value=EnvType.CLIENT)
public enum PresenceSharing implements StringRepresentable
{
    NONE("none"),
    LIMITED("limited"),
    ALL("all");

    public static final Codec<PresenceSharing> CODEC;
    public static final String TRANSLATION_KEY_BASE = "options.sharePresence";
    private final String name;
    private final Component translatable;
    private final Component tooltip;

    private PresenceSharing(String name) {
        this.name = name;
        this.translatable = Component.translatable("options.sharePresence." + name);
        this.tooltip = Component.translatable("options.sharePresence." + name + ".tooltip");
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component getTranslation() {
        return this.translatable;
    }

    public Component getTooltip() {
        return this.tooltip;
    }

    static {
        CODEC = StringRepresentable.fromEnum(PresenceSharing::values);
    }
}

