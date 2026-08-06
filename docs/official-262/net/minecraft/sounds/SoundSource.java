/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.sounds;

public enum SoundSource {
    MASTER("master"),
    MUSIC("music"),
    RECORDS("record"),
    WEATHER("weather"),
    BLOCKS("block"),
    HOSTILE("hostile"),
    NEUTRAL("neutral"),
    PLAYERS("player"),
    AMBIENT("ambient"),
    VOICE("voice"),
    UI("ui");

    private final String name;

    private SoundSource(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}

