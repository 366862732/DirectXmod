package com.dx12.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class D3D12Bridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(D3D12Bridge.class);
    private static boolean initialized = false;
    
    static {
        // 静态块会在类首次加载时执行，但实际�?native 库已经在 Dx12Mod 中加载了
        LOGGER.info("D3D12Bridge class loaded");
    }
    
    /**
     * 初始�?D3D12 渲染�?
     * @param hwnd Windows 窗口句柄
     * @param width 窗口宽度
     * @param height 窗口高度
     * @return 是否初始化成�?
     */
    public static native boolean nativeInit(long hwnd, int width, int height);
    
    /**
     * 销�?D3D12 渲染�?
     */
    public static native void nativeDestroy();
    
    /**
     * 渲染一�?
     */
    public static native void nativeRender();
    
    /**
     * 交换�?Present
     */
    public static native void nativePresent();
    
    /**
     * 窗口大小改变时调�?
     * @param width 新宽�?
     * @param height 新高�?
     */
    public static native void nativeResize(int width, int height);
    
    // 便捷方法：检查是否已初始�?
    public static boolean isInitialized() {
        return initialized;
    }
    
    // 内部调用，由 native 代码回调设置状�?
    public static void setInitialized(boolean state) {
        initialized = state;
        LOGGER.info("D3D12Bridge initialized state changed to: {}", state);
    }
}
