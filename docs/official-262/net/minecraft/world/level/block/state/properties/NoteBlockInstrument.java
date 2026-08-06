/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.state.properties;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;

public enum NoteBlockInstrument implements StringRepresentable
{
    HARP("harp", SoundEvents.NOTE_BLOCK_HARP, Type.BASE_BLOCK),
    BASEDRUM("basedrum", SoundEvents.NOTE_BLOCK_BASEDRUM, Type.BASE_BLOCK),
    SNARE("snare", SoundEvents.NOTE_BLOCK_SNARE, Type.BASE_BLOCK),
    HAT("hat", SoundEvents.NOTE_BLOCK_HAT, Type.BASE_BLOCK),
    BASS("bass", SoundEvents.NOTE_BLOCK_BASS, Type.BASE_BLOCK),
    FLUTE("flute", SoundEvents.NOTE_BLOCK_FLUTE, Type.BASE_BLOCK),
    BELL("bell", SoundEvents.NOTE_BLOCK_BELL, Type.BASE_BLOCK),
    GUITAR("guitar", SoundEvents.NOTE_BLOCK_GUITAR, Type.BASE_BLOCK),
    CHIME("chime", SoundEvents.NOTE_BLOCK_CHIME, Type.BASE_BLOCK),
    XYLOPHONE("xylophone", SoundEvents.NOTE_BLOCK_XYLOPHONE, Type.BASE_BLOCK),
    IRON_XYLOPHONE("iron_xylophone", SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, Type.BASE_BLOCK),
    COW_BELL("cow_bell", SoundEvents.NOTE_BLOCK_COW_BELL, Type.BASE_BLOCK),
    DIDGERIDOO("didgeridoo", SoundEvents.NOTE_BLOCK_DIDGERIDOO, Type.BASE_BLOCK),
    BIT("bit", SoundEvents.NOTE_BLOCK_BIT, Type.BASE_BLOCK),
    BANJO("banjo", SoundEvents.NOTE_BLOCK_BANJO, Type.BASE_BLOCK),
    PLING("pling", SoundEvents.NOTE_BLOCK_PLING, Type.BASE_BLOCK),
    TRUMPET("trumpet", SoundEvents.NOTE_BLOCK_TRUMPET, Type.BASE_BLOCK),
    TRUMPET_EXPOSED("trumpet_exposed", SoundEvents.NOTE_BLOCK_TRUMPET_EXPOSED, Type.BASE_BLOCK),
    TRUMPET_OXIDIZED("trumpet_oxidized", SoundEvents.NOTE_BLOCK_TRUMPET_OXIDIZED, Type.BASE_BLOCK),
    TRUMPET_WEATHERED("trumpet_weathered", SoundEvents.NOTE_BLOCK_TRUMPET_WEATHERED, Type.BASE_BLOCK),
    ZOMBIE("zombie", SoundEvents.NOTE_BLOCK_IMITATE_ZOMBIE, Type.MOB_HEAD),
    SKELETON("skeleton", SoundEvents.NOTE_BLOCK_IMITATE_SKELETON, Type.MOB_HEAD),
    CREEPER("creeper", SoundEvents.NOTE_BLOCK_IMITATE_CREEPER, Type.MOB_HEAD),
    DRAGON("dragon", SoundEvents.NOTE_BLOCK_IMITATE_ENDER_DRAGON, Type.MOB_HEAD),
    WITHER_SKELETON("wither_skeleton", SoundEvents.NOTE_BLOCK_IMITATE_WITHER_SKELETON, Type.MOB_HEAD),
    PIGLIN("piglin", SoundEvents.NOTE_BLOCK_IMITATE_PIGLIN, Type.MOB_HEAD),
    CUSTOM_HEAD("custom_head", SoundEvents.UI_BUTTON_CLICK, Type.CUSTOM);

    private final String name;
    private final Holder<SoundEvent> soundEvent;
    private final Type type;

    private NoteBlockInstrument(String name, Holder<SoundEvent> soundEvent, Type type) {
        this.name = name;
        this.soundEvent = soundEvent;
        this.type = type;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Holder<SoundEvent> getSoundEvent() {
        return this.soundEvent;
    }

    public boolean isTunable() {
        return this.type == Type.BASE_BLOCK;
    }

    public boolean hasCustomSound() {
        return this.type == Type.CUSTOM;
    }

    public boolean worksAboveNoteBlock() {
        return this.type != Type.BASE_BLOCK;
    }

    private static enum Type {
        BASE_BLOCK,
        MOB_HEAD,
        CUSTOM;

    }
}

