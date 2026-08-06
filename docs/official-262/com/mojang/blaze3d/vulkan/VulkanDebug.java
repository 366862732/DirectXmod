/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.vulkan.EXTDebugUtils
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkDebugUtilsLabelEXT
 *  org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
 *  org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
 *  org.lwjgl.vulkan.VkDebugUtilsObjectNameInfoEXT
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkInstance
 *  org.lwjgl.vulkan.VkInstanceCreateInfo
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.vulkan.VulkanUtils;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDebugUtilsLabelEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugUtilsObjectNameInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public interface VulkanDebug {
    public static VulkanDebug create(int verbosity, boolean wantsDebugLabels, Set<String> availableExtensions, Set<String> enabledExtensions) {
        if ((verbosity > 0 || wantsDebugLabels) && availableExtensions.contains("VK_EXT_debug_utils")) {
            enabledExtensions.add("VK_EXT_debug_utils");
            return new Enabled(verbosity, wantsDebugLabels);
        }
        return new Disabled();
    }

    public void chainCreateInfo(VkInstanceCreateInfo var1, MemoryStack var2);

    public void setup(VkInstance var1);

    public void setObjectName(VkDevice var1, int var2, long var3, String var5);

    public void setObjectName(VkDevice var1, int var2, long var3, Supplier<String> var5);

    public void beginDebugGroup(VkCommandBuffer var1, Supplier<String> var2);

    public void endDebugGroup(VkCommandBuffer var1);

    public void destroy(VkInstance var1);

    public boolean enabled();

    @Environment(value=EnvType.CLIENT)
    public static class Enabled
    implements VulkanDebug {
        private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), 3);
        private static final Logger LOGGER = LogUtils.getLogger();
        public static final int MESSAGE_TYPE_BITMASK = 7;
        private static final int[] DEBUG_LEVELS = new int[]{4096, 256, 16, 1};
        private final boolean wantsDebugLabels;
        private long messenger;
        private final int severityBitmask;

        public Enabled(int verbosity, boolean wantsDebugLabels) {
            this.wantsDebugLabels = wantsDebugLabels;
            int severityBitmask = 0;
            if (verbosity > 0) {
                for (int i = 0; i < Math.min(verbosity, DEBUG_LEVELS.length); ++i) {
                    severityBitmask |= DEBUG_LEVELS[i];
                }
            }
            this.severityBitmask = severityBitmask;
        }

        @Override
        public void chainCreateInfo(VkInstanceCreateInfo instanceCreateInfo, MemoryStack stack) {
            if (this.severityBitmask > 0) {
                instanceCreateInfo.pNext(VkDebugUtilsMessengerCreateInfoEXT.calloc((MemoryStack)stack).sType$Default().messageSeverity(this.severityBitmask).messageType(7).pfnUserCallback(this::onDebugMessage));
            }
        }

        @Override
        public void setup(VkInstance vkInstance) {
            try (MemoryStack stack = MemoryStack.stackPush();){
                LongBuffer pointer = stack.callocLong(1);
                VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc((MemoryStack)stack).sType$Default().messageSeverity(this.severityBitmask).messageType(7).pfnUserCallback(this::onDebugMessage);
                int result = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT((VkInstance)vkInstance, (VkDebugUtilsMessengerCreateInfoEXT)createInfo, null, (LongBuffer)pointer);
                if (result != 0) {
                    LOGGER.error("Error creating debug utils messenger: {}", (Object)VulkanUtils.resultToString(result));
                    return;
                }
                this.messenger = pointer.get(0);
            }
        }

        @Override
        public void setObjectName(VkDevice device, int objectType, long objectHandle, String label) {
            if (!this.wantsDebugLabels) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush();){
                ByteBuffer name = stack.UTF8((CharSequence)label);
                VkDebugUtilsObjectNameInfoEXT nameInfo = VkDebugUtilsObjectNameInfoEXT.calloc((MemoryStack)stack).sType$Default().pObjectName(name).objectType(objectType).objectHandle(objectHandle);
                EXTDebugUtils.vkSetDebugUtilsObjectNameEXT((VkDevice)device, (VkDebugUtilsObjectNameInfoEXT)nameInfo);
            }
        }

        @Override
        public void setObjectName(VkDevice device, int objectType, long objectHandle, Supplier<String> label) {
            if (!this.wantsDebugLabels) {
                return;
            }
            this.setObjectName(device, objectType, objectHandle, label.get());
        }

        private int onDebugMessage(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
            VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create((long)pCallbackData);
            String message = callbackData.pMessageString();
            if ((messageSeverity & 0x10) != 0) {
                LOGGER.info("{}", (Object)message);
            } else if ((messageSeverity & 0x100) != 0) {
                LOGGER.warn("{}", (Object)message);
            } else {
                if ((messageSeverity & 0x1000) != 0) {
                    if (message != null && (message.contains("vkDestroyInstance") || message.contains("vkDestroyDevice"))) {
                        LOGGER.error("{}", (Object)message);
                    } else {
                        String callStack = STACK_WALKER.walk(s -> s.filter(frame -> frame.getDeclaringClass() != Enabled.class && !frame.getDeclaringClass().getPackageName().startsWith("org.lwjgl")).limit(5L).map(frame -> "\t" + String.valueOf(frame)).collect(Collectors.joining("\n")));
                        LOGGER.error("{}\n{}", (Object)message, (Object)callStack);
                    }
                    return 1;
                }
                LOGGER.debug("{}", (Object)message);
            }
            return 0;
        }

        @Override
        public void beginDebugGroup(VkCommandBuffer buffer, Supplier<String> label) {
            if (!this.wantsDebugLabels) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush();){
                ByteBuffer name = stack.UTF8((CharSequence)label.get());
                VkDebugUtilsLabelEXT nameInfo = VkDebugUtilsLabelEXT.calloc((MemoryStack)stack).sType$Default().pLabelName(name);
                EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT((VkCommandBuffer)buffer, (VkDebugUtilsLabelEXT)nameInfo);
            }
        }

        @Override
        public void endDebugGroup(VkCommandBuffer buffer) {
            if (!this.wantsDebugLabels) {
                return;
            }
            EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT((VkCommandBuffer)buffer);
        }

        @Override
        public void destroy(VkInstance instance) {
            if (this.messenger != 0L) {
                EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT((VkInstance)instance, (long)this.messenger, null);
            }
        }

        @Override
        public boolean enabled() {
            return true;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Disabled
    implements VulkanDebug {
        @Override
        public void chainCreateInfo(VkInstanceCreateInfo instanceCreateInfo, MemoryStack stack) {
        }

        @Override
        public void setup(VkInstance vkInstance) {
        }

        @Override
        public void setObjectName(VkDevice device, int objectType, long objectHandle, String label) {
        }

        @Override
        public void setObjectName(VkDevice device, int objectType, long objectHandle, Supplier<String> label) {
        }

        @Override
        public void beginDebugGroup(VkCommandBuffer buffer, Supplier<String> label) {
        }

        @Override
        public void endDebugGroup(VkCommandBuffer buffer) {
        }

        @Override
        public void destroy(VkInstance instance) {
        }

        @Override
        public boolean enabled() {
            return false;
        }
    }
}

