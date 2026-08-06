/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public final class SoundPreviewHandler {
    private static @Nullable SoundInstance activePreview;
    private static @Nullable SoundSource previousCategory;

    public static void preview(SoundManager soundManager, SoundSource category, float volume) {
        SoundPreviewHandler.stopOtherCategoryPreview(soundManager, category);
        if (SoundPreviewHandler.canPlaySound(soundManager)) {
            SoundEvent previewSound;
            switch (category) {
                case RECORDS: {
                    SoundEvent soundEvent = SoundEvents.NOTE_BLOCK_GUITAR.value();
                    break;
                }
                case WEATHER: {
                    SoundEvent soundEvent = SoundEvents.LIGHTNING_BOLT_THUNDER;
                    break;
                }
                case BLOCKS: {
                    SoundEvent soundEvent = SoundEvents.GRASS_PLACE;
                    break;
                }
                case HOSTILE: {
                    SoundEvent soundEvent = SoundEvents.ZOMBIE_AMBIENT;
                    break;
                }
                case NEUTRAL: {
                    SoundEvent soundEvent = SoundEvents.COW_SOUNDS.get((Object)CowSoundVariants.SoundSet.CLASSIC).ambientSound().value();
                    break;
                }
                case PLAYERS: {
                    SoundEvent soundEvent = SoundEvents.GENERIC_EAT.value();
                    break;
                }
                case AMBIENT: {
                    SoundEvent soundEvent = SoundEvents.AMBIENT_CAVE.value();
                    break;
                }
                case UI: {
                    SoundEvent soundEvent = SoundEvents.UI_BUTTON_CLICK.value();
                    break;
                }
                default: {
                    SoundEvent soundEvent = previewSound = SoundEvents.EMPTY;
                }
            }
            if (previewSound != SoundEvents.EMPTY) {
                activePreview = SimpleSoundInstance.forUI(previewSound, 1.0f, volume);
                soundManager.play(activePreview);
            }
        }
    }

    private static void stopOtherCategoryPreview(SoundManager soundManager, SoundSource category) {
        if (previousCategory != category) {
            previousCategory = category;
            if (activePreview != null) {
                soundManager.stop(activePreview);
            }
        }
    }

    private static boolean canPlaySound(SoundManager soundManager) {
        return activePreview == null || !soundManager.isActive(activePreview);
    }
}

