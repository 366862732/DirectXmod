/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.openal.AL10
 *  org.lwjgl.openal.ALC10
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class OpenAlUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static String alErrorToString(int error) {
        return switch (error) {
            case 40961 -> "Invalid name parameter.";
            case 40962 -> "Invalid enumerated parameter value.";
            case 40963 -> "Invalid parameter parameter value.";
            case 40964 -> "Invalid operation.";
            case 40965 -> "Unable to allocate memory.";
            default -> "An unrecognized error occurred.";
        };
    }

    public static boolean checkALError(String location) {
        int error = AL10.alGetError();
        if (error != 0) {
            LOGGER.error("{}: {}", (Object)location, (Object)OpenAlUtil.alErrorToString(error));
            return true;
        }
        return false;
    }

    private static String alcErrorToString(int error) {
        return switch (error) {
            case 40961 -> "Invalid device.";
            case 40962 -> "Invalid context.";
            case 40964 -> "Invalid value.";
            case 40963 -> "Illegal enum.";
            case 40965 -> "Unable to allocate memory.";
            default -> "An unrecognized error occurred.";
        };
    }

    public static boolean checkALCError(long device, String location) {
        int error = ALC10.alcGetError((long)device);
        if (error != 0) {
            LOGGER.error("{} ({}): {}", new Object[]{location, device, OpenAlUtil.alcErrorToString(error)});
            return true;
        }
        return false;
    }

    public static int audioFormatToOpenAl(AudioFormat audioFormat) {
        AudioFormat.Encoding encoding = audioFormat.getEncoding();
        int channels = audioFormat.getChannels();
        int sampleSizeInBits = audioFormat.getSampleSizeInBits();
        if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED) || encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            if (channels == 1) {
                if (sampleSizeInBits == 8) {
                    return 4352;
                }
                if (sampleSizeInBits == 16) {
                    return 4353;
                }
            } else if (channels == 2) {
                if (sampleSizeInBits == 8) {
                    return 4354;
                }
                if (sampleSizeInBits == 16) {
                    return 4355;
                }
            }
        }
        throw new IllegalArgumentException("Invalid audio format: " + String.valueOf(audioFormat));
    }
}

