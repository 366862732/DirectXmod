/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 *  org.lwjgl.vulkan.VkClearColorValue
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.GpuDeviceLossException;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;
import org.lwjgl.vulkan.VkClearColorValue;

@Environment(value=EnvType.CLIENT)
public class VulkanUtils {
    public static final Set<DeviceUUID> KNOWN_PROBLEMATIC_DEVICES = Set.of(new DeviceUUID(14, 32902, 338), new DeviceUUID(14, 32902, 341), new DeviceUUID(14, 32902, 343), new DeviceUUID(14, 32902, 346), new DeviceUUID(14, 32902, 354), new DeviceUUID(14, 32902, 358), new DeviceUUID(14, 32902, 362), new DeviceUUID(14, 32902, 1026), new DeviceUUID(14, 32902, 1030), new DeviceUUID(14, 32902, 1034), new DeviceUUID(14, 32902, 1035), new DeviceUUID(14, 32902, 1038), new DeviceUUID(14, 32902, 1042), new DeviceUUID(14, 32902, 1046), new DeviceUUID(14, 32902, 1050), new DeviceUUID(14, 32902, 1051), new DeviceUUID(14, 32902, 1054), new DeviceUUID(14, 32902, 1058), new DeviceUUID(14, 32902, 1062), new DeviceUUID(14, 32902, 1066), new DeviceUUID(14, 32902, 1067), new DeviceUUID(14, 32902, 1070), new DeviceUUID(14, 32902, 2562), new DeviceUUID(14, 32902, 2566), new DeviceUUID(14, 32902, 2570), new DeviceUUID(14, 32902, 2571), new DeviceUUID(14, 32902, 2574), new DeviceUUID(14, 32902, 2578), new DeviceUUID(14, 32902, 2582), new DeviceUUID(14, 32902, 2586), new DeviceUUID(14, 32902, 2587), new DeviceUUID(14, 32902, 2590), new DeviceUUID(14, 32902, 2594), new DeviceUUID(14, 32902, 2598), new DeviceUUID(14, 32902, 2602), new DeviceUUID(14, 32902, 2603), new DeviceUUID(14, 32902, 2606), new DeviceUUID(14, 32902, 3362), new DeviceUUID(14, 32902, 3366), new DeviceUUID(14, 32902, 3370), new DeviceUUID(14, 32902, 3371), new DeviceUUID(14, 32902, 3374), new DeviceUUID(14, 32902, 3888), new DeviceUUID(14, 32902, 3889), new DeviceUUID(14, 32902, 3890), new DeviceUUID(14, 32902, 3891), new DeviceUUID(14, 32902, 5638), new DeviceUUID(14, 32902, 5650), new DeviceUUID(14, 32902, 5654), new DeviceUUID(14, 32902, 5662), new DeviceUUID(14, 32902, 5666), new DeviceUUID(14, 32902, 5670), new DeviceUUID(14, 32902, 5674), new DeviceUUID(14, 32902, 5675), new DeviceUUID(14, 32902, 8880), new DeviceUUID(14, 32902, 8881), new DeviceUUID(14, 32902, 8882), new DeviceUUID(14, 32902, 8883));

    public static void throwIfFailure(int result, String message, BackendCreationException.Reason reason) throws BackendCreationException {
        if (result < 0) {
            throw new BackendCreationException(VulkanUtils.resultToString(result) + ": " + message, reason);
        }
    }

    public static void crashIfFailure(VulkanDevice device, int result, String message) {
        if (result < 0) {
            String error = VulkanUtils.resultToString(result) + ": " + message;
            if (result == -4) {
                List<CheckpointExtension.QueueCheckpoints> checkpoints = device.checkpointExtension().retrieveCheckpoints(true);
                throw new GpuDeviceLossException(error + "\n" + VulkanUtils.formatCheckpoints(checkpoints));
            }
            throw new IllegalStateException(error);
        }
    }

    public static String formatCheckpoints(List<CheckpointExtension.QueueCheckpoints> queueCheckpoints) {
        StringBuilder result = new StringBuilder();
        for (CheckpointExtension.QueueCheckpoints queue : queueCheckpoints) {
            result.append("Queue 0x").append(Long.toHexString(queue.queue())).append('\n');
            for (CheckpointExtension.StageCheckpoint checkpoint : queue.checkpoints()) {
                result.append(' ').append(VulkanUtils.pipelineStageToString(checkpoint.stage())).append(" = ").append((Object)checkpoint.type()).append(' ').append(checkpoint.label()).append('\n');
            }
        }
        return result.toString();
    }

    public static String pipelineStageToString(long pipelineStage) {
        if (pipelineStage == 0L) {
            return "NONE";
        }
        if (pipelineStage == 1L) {
            return "TOP_OF_PIPE";
        }
        if (pipelineStage == 2L) {
            return "DRAW_INDIRECT";
        }
        if (pipelineStage == 4L) {
            return "VERTEX_INPUT";
        }
        if (pipelineStage == 8L) {
            return "VERTEX_SHADER";
        }
        if (pipelineStage == 16L) {
            return "TESSELLATION_CONTROL_SHADER";
        }
        if (pipelineStage == 32L) {
            return "TESSELLATION_EVALUATION_SHADER";
        }
        if (pipelineStage == 64L) {
            return "GEOMETRY_SHADER";
        }
        if (pipelineStage == 128L) {
            return "FRAGMENT_SHADER";
        }
        if (pipelineStage == 256L) {
            return "EARLY_FRAGMENT_TESTS";
        }
        if (pipelineStage == 512L) {
            return "LATE_FRAGMENT_TESTS";
        }
        if (pipelineStage == 1024L) {
            return "COLOR_ATTACHMENT_OUTPUT";
        }
        if (pipelineStage == 2048L) {
            return "COMPUTE_SHADER";
        }
        if (pipelineStage == 4096L) {
            return "TRANSFER";
        }
        if (pipelineStage == 8192L) {
            return "BOTTOM_OF_PIPE";
        }
        if (pipelineStage == 16384L) {
            return "HOST";
        }
        if (pipelineStage == 32768L) {
            return "ALL_GRAPHICS";
        }
        if (pipelineStage == 65536L) {
            return "ALL_COMMANDS";
        }
        if (pipelineStage == 131072L) {
            return "COMMAND_PREPROCESS";
        }
        if (pipelineStage == 262144L) {
            return "CONDITIONAL_RENDERING";
        }
        if (pipelineStage == 524288L) {
            return "TASK_SHADER";
        }
        if (pipelineStage == 0x100000L) {
            return "MESH_SHADER";
        }
        if (pipelineStage == 0x200000L) {
            return "RAY_TRACING_SHADER";
        }
        if (pipelineStage == 0x400000L) {
            return "FRAGMENT_SHADING_RATE_ATTACHMENT";
        }
        if (pipelineStage == 0x800000L) {
            return "FRAGMENT_DENSITY_PROCESS";
        }
        if (pipelineStage == 0x1000000L) {
            return "TRANSFORM_FEEDBACK";
        }
        if (pipelineStage == 0x2000000L) {
            return "ACCELERATION_STRUCTURE_BUILD";
        }
        if (pipelineStage == 0x100000000L) {
            return "COPY";
        }
        if (pipelineStage == 0x200000000L) {
            return "RESOLVE";
        }
        if (pipelineStage == 0x400000000L) {
            return "BLIT";
        }
        if (pipelineStage == 0x800000000L) {
            return "CLEAR";
        }
        if (pipelineStage == 0x1000000000L) {
            return "INDEX_INPUT";
        }
        if (pipelineStage == 0x2000000000L) {
            return "VERTEX_ATTRIBUTE_INPUT";
        }
        if (pipelineStage == 0x4000000000L) {
            return "PRE_RASTERIZATION_SHADERS";
        }
        return "0x" + Long.toHexString(pipelineStage);
    }

    public static String resultToString(int error) {
        return switch (error) {
            case 0 -> "VK_SUCCESS";
            case 1 -> "VK_NOT_READY";
            case 2 -> "VK_TIMEOUT";
            case 3 -> "VK_EVENT_SET";
            case 4 -> "VK_EVENT_RESET";
            case 5 -> "VK_INCOMPLETE";
            case -1 -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case -2 -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case -3 -> "VK_ERROR_INITIALIZATION_FAILED";
            case -4 -> "VK_ERROR_DEVICE_LOST";
            case -5 -> "VK_ERROR_MEMORY_MAP_FAILED";
            case -6 -> "VK_ERROR_LAYER_NOT_PRESENT";
            case -7 -> "VK_ERROR_EXTENSION_NOT_PRESENT";
            case -8 -> "VK_ERROR_FEATURE_NOT_PRESENT";
            case -9 -> "VK_ERROR_INCOMPATIBLE_DRIVER";
            case -10 -> "VK_ERROR_TOO_MANY_OBJECTS";
            case -11 -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case -12 -> "VK_ERROR_FRAGMENTED_POOL";
            case -13 -> "VK_ERROR_UNKNOWN";
            case -1000069000 -> "VK_ERROR_OUT_OF_POOL_MEMORY";
            case -1000072003 -> "VK_ERROR_INVALID_EXTERNAL_HANDLE";
            case -1000257000 -> "VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS";
            case -1000161000 -> "VK_ERROR_FRAGMENTATION";
            case -1000000000 -> "VK_ERROR_SURFACE_LOST_KHR";
            case -1000000001 -> "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
            case 1000001003 -> "VK_SUBOPTIMAL_KHR";
            case -1000001004 -> "VK_ERROR_OUT_OF_DATE_KHR";
            default -> "0x" + Integer.toHexString(error);
        };
    }

    public static boolean hasAllBits(int bitfield, int bitmask) {
        return (bitfield & bitmask) == bitmask;
    }

    public static boolean hasAllBits(long bitfield, long bitmask) {
        return (bitfield & bitmask) == bitmask;
    }

    public static boolean hasAnyBit(int bitfield, int bitmask) {
        return (bitfield & bitmask) != 0;
    }

    public static boolean hasAnyBit(long bitfield, long bitmask) {
        return (bitfield & bitmask) != 0L;
    }

    public static boolean hasNoBit(int bitfield, int bitmask) {
        return (bitfield & bitmask) == 0;
    }

    public static boolean hasNoBit(long bitfield, long bitmask) {
        return (bitfield & bitmask) == 0L;
    }

    public static VkClearColorValue putArgb(VkClearColorValue vkClearColor, Vector4fc argb) {
        vkClearColor.float32(0, argb.x());
        vkClearColor.float32(1, argb.y());
        vkClearColor.float32(2, argb.z());
        vkClearColor.float32(3, argb.w());
        return vkClearColor;
    }

    @Environment(value=EnvType.CLIENT)
    public record DeviceUUID(int driverID, int vendorID, int deviceID) {
    }
}

