/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.item.JukeboxSong;

public interface JukeboxSongs {
    public static final ResourceKey<JukeboxSong> THIRTEEN = JukeboxSongs.create("13");
    public static final ResourceKey<JukeboxSong> CAT = JukeboxSongs.create("cat");
    public static final ResourceKey<JukeboxSong> BLOCKS = JukeboxSongs.create("blocks");
    public static final ResourceKey<JukeboxSong> CHIRP = JukeboxSongs.create("chirp");
    public static final ResourceKey<JukeboxSong> FAR = JukeboxSongs.create("far");
    public static final ResourceKey<JukeboxSong> MALL = JukeboxSongs.create("mall");
    public static final ResourceKey<JukeboxSong> MELLOHI = JukeboxSongs.create("mellohi");
    public static final ResourceKey<JukeboxSong> STAL = JukeboxSongs.create("stal");
    public static final ResourceKey<JukeboxSong> STRAD = JukeboxSongs.create("strad");
    public static final ResourceKey<JukeboxSong> WARD = JukeboxSongs.create("ward");
    public static final ResourceKey<JukeboxSong> ELEVEN = JukeboxSongs.create("11");
    public static final ResourceKey<JukeboxSong> WAIT = JukeboxSongs.create("wait");
    public static final ResourceKey<JukeboxSong> PIGSTEP = JukeboxSongs.create("pigstep");
    public static final ResourceKey<JukeboxSong> OTHERSIDE = JukeboxSongs.create("otherside");
    public static final ResourceKey<JukeboxSong> FIVE = JukeboxSongs.create("5");
    public static final ResourceKey<JukeboxSong> RELIC = JukeboxSongs.create("relic");
    public static final ResourceKey<JukeboxSong> PRECIPICE = JukeboxSongs.create("precipice");
    public static final ResourceKey<JukeboxSong> CREATOR = JukeboxSongs.create("creator");
    public static final ResourceKey<JukeboxSong> CREATOR_MUSIC_BOX = JukeboxSongs.create("creator_music_box");
    public static final ResourceKey<JukeboxSong> TEARS = JukeboxSongs.create("tears");
    public static final ResourceKey<JukeboxSong> LAVA_CHICKEN = JukeboxSongs.create("lava_chicken");
    public static final ResourceKey<JukeboxSong> BOUNCE = JukeboxSongs.create("bounce");

    private static ResourceKey<JukeboxSong> create(String id) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.withDefaultNamespace(id));
    }

    private static void register(BootstrapContext<JukeboxSong> context, ResourceKey<JukeboxSong> registryKey, Holder.Reference<SoundEvent> soundEvent, int lengthInSeconds, int comparatorOutput) {
        context.register(registryKey, new JukeboxSong(soundEvent, Component.translatable(Util.makeDescriptionId("jukebox_song", registryKey.identifier())), lengthInSeconds, comparatorOutput));
    }

    public static void bootstrap(BootstrapContext<JukeboxSong> context) {
        JukeboxSongs.register(context, THIRTEEN, SoundEvents.MUSIC_DISC_13, 178, 1);
        JukeboxSongs.register(context, CAT, SoundEvents.MUSIC_DISC_CAT, 185, 2);
        JukeboxSongs.register(context, BLOCKS, SoundEvents.MUSIC_DISC_BLOCKS, 345, 3);
        JukeboxSongs.register(context, CHIRP, SoundEvents.MUSIC_DISC_CHIRP, 185, 4);
        JukeboxSongs.register(context, FAR, SoundEvents.MUSIC_DISC_FAR, 174, 5);
        JukeboxSongs.register(context, MALL, SoundEvents.MUSIC_DISC_MALL, 197, 6);
        JukeboxSongs.register(context, MELLOHI, SoundEvents.MUSIC_DISC_MELLOHI, 96, 7);
        JukeboxSongs.register(context, STAL, SoundEvents.MUSIC_DISC_STAL, 150, 8);
        JukeboxSongs.register(context, STRAD, SoundEvents.MUSIC_DISC_STRAD, 188, 9);
        JukeboxSongs.register(context, WARD, SoundEvents.MUSIC_DISC_WARD, 251, 10);
        JukeboxSongs.register(context, ELEVEN, SoundEvents.MUSIC_DISC_11, 71, 11);
        JukeboxSongs.register(context, WAIT, SoundEvents.MUSIC_DISC_WAIT, 238, 12);
        JukeboxSongs.register(context, PIGSTEP, SoundEvents.MUSIC_DISC_PIGSTEP, 149, 13);
        JukeboxSongs.register(context, OTHERSIDE, SoundEvents.MUSIC_DISC_OTHERSIDE, 195, 14);
        JukeboxSongs.register(context, FIVE, SoundEvents.MUSIC_DISC_5, 178, 15);
        JukeboxSongs.register(context, RELIC, SoundEvents.MUSIC_DISC_RELIC, 218, 14);
        JukeboxSongs.register(context, PRECIPICE, SoundEvents.MUSIC_DISC_PRECIPICE, 299, 13);
        JukeboxSongs.register(context, CREATOR, SoundEvents.MUSIC_DISC_CREATOR, 176, 12);
        JukeboxSongs.register(context, CREATOR_MUSIC_BOX, SoundEvents.MUSIC_DISC_CREATOR_MUSIC_BOX, 73, 11);
        JukeboxSongs.register(context, TEARS, SoundEvents.MUSIC_DISC_TEARS, 175, 10);
        JukeboxSongs.register(context, LAVA_CHICKEN, SoundEvents.MUSIC_DISC_LAVA_CHICKEN, 134, 9);
        JukeboxSongs.register(context, BOUNCE, SoundEvents.MUSIC_DISC_BOUNCE, 234, 8);
    }
}

