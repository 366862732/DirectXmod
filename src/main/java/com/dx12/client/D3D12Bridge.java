package com.dx12.client;

import com.dx12.DX12LibClient;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central bridge between Mixin-intercepted OpenGL calls and the native D3D12 backend.
 */
public class D3D12Bridge {

    private static final AtomicInteger bufferIdCounter = new AtomicInteger(1);

    // ===== Native JNI lifecycle =====
    public static boolean nativeInit(long hwnd, int width, int height) {
        return DX12LibClient.nativeInit(hwnd, width, height);
    }

    public static void nativeDestroy() {
        DX12LibClient.nativeDestroy();
    }

    public static void nativeRender() {
        DX12LibClient.nativeRender();
    }

    public static void nativePresent() {
        DX12LibClient.nativePresent();
    }

    public static void nativeResize(int width, int height) {
        DX12LibClient.nativeResize(width, height);
    }

    // ===== GL State Operations =====

    private static float clearR = 0.0f, clearG = 0.0f, clearB = 0.0f, clearA = 1.0f;

    public static void glClear(int mask) {
        // Forward as a full render + present cycle
        DX12LibClient.nativeSetClearColor(clearR, clearG, clearB, clearA);
        DX12LibClient.nativeRender();
    }

    public static void glClearColor(float r, float g, float b, float a) {
        clearR = r;
        clearG = g;
        clearB = b;
        clearA = a;
    }

    // ===== GL Buffer Operations =====

    public static int glGenBuffers() {
        int id = bufferIdCounter.getAndIncrement();
        return id;
    }

    public static void glBindBuffer(int target, int buffer) {
        // Track in D3D12 state (future: map to D3D12 resources)
    }

    public static void glBufferData(int target, ByteBuffer data, int usage) {
        // Future: upload to D3D12 buffer
    }

    // ===== GL Draw Operations =====

    public static void glDrawArrays(int mode, int first, int count) {
        // Future: issue D3D12 draw call
    }
}
