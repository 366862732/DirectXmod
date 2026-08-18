package com.dx12.d3d12;

import com.dx12.dx12.Dx12Native;

/**
 * Full DX12 context: device + command queue + swap chain.
 *
 * Lifecycle:
 * 1. {@link #create()} — initializes the global D3D12 device and command queue
 * 2. {@link #createSurface(long)} — creates a swap chain for the given HWND
 * 3. Use {@link #getQueueHandle()} / {@link #getDeviceHandle()} for low-level access
 * 4. {@link #close()} — releases the surface (queue/device persist until JVM exit)
 *
 * The underlying C++ layer manages the actual COM object lifetimes.
 */
public class Dx12DeviceContext implements AutoCloseable {

    private final long queueHandle;
    private final long deviceHandle;
    private final long timestampFrequency;
    private long surfaceHandle = 0L;
    private long hwnd = 0L;
    private int width = 0;
    private int height = 0;

    private Dx12DeviceContext(long queue, long device, long freq) {
        this.queueHandle = queue;
        this.deviceHandle = device;
        this.timestampFrequency = freq;
    }

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Create a Dx12DeviceContext. Calls dx12CreateDevice() which initializes
     * the global device + command queue.
     *
     * @return the context, or null if device creation failed
     */
    public static Dx12DeviceContext create() {
        String result = Dx12Native.dx12CreateDevice();
        if (result == null || result.startsWith("ERROR:")) {
            System.err.println("[dx12] Device creation failed: " + result);
            return null;
        }
        long q = Dx12Native.dx12GetQueueHandle();
        long d = Dx12Native.dx12GetDeviceHandle();
        long freq = Dx12Native.dx12GetTimestampFrequency();
        System.out.printf("  Queue=0x%016x Device=0x%016x Freq=%d ticks/s%n",
            q, d, freq);
        return new Dx12DeviceContext(q, d, freq);
    }

    /**
     * Create a swap chain for the given native HWND.
     *
     * @param hwnd native window handle (long = HWND)
     * @return true on success
     */
    public boolean createSurface(long hwnd) {
        long surface = Dx12Native.dx12CreateSurface(hwnd);
        if (surface == 0) {
            System.err.println("[dx12] createSurface failed for HWND=0x" + Long.toHexString(hwnd));
            return false;
        }
        this.surfaceHandle = surface;
        this.hwnd = hwnd;
        System.out.printf("  Surface created (handle=0x%016x)%n", surface);
        return true;
    }

    /**
     * Resize the swap chain to the given dimensions.
     *
     * @param width  buffer width
     * @param height buffer height
     * @param presentMode 0=IMMEDIATE, 2=FIFO, 3=FIFO_RELAXED
     * @return true on success
     */
    public boolean configureSurface(int width, int height, int presentMode) {
        if (surfaceHandle == 0) {
            System.err.println("[dx12] No surface configured");
            return false;
        }
        boolean ok = Dx12Native.dx12ConfigureSurface(surfaceHandle, width, height, presentMode);
        if (ok) {
            this.width = width;
            this.height = height;
            System.out.printf("  Surface resized: %dx%d mode=%d%n", width, height, presentMode);
        } else {
            System.err.printf("[dx12] configureSurface %dx%d failed%n", width, height);
        }
        return ok;
    }

    /** Acquire the next back buffer index. @return true on success */
    public boolean acquireSurface() {
        if (surfaceHandle == 0) return false;
        return Dx12Native.dx12AcquireSurface(surfaceHandle);
    }

    /** Present the current back buffer. */
    public void presentSurface() {
        if (surfaceHandle != 0) Dx12Native.dx12PresentSurface(surfaceHandle);
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public long getQueueHandle() { return queueHandle; }
    public long getDeviceHandle() { return deviceHandle; }
    public long getTimestampFrequency() { return timestampFrequency; }
    public long getSurfaceHandle() { return surfaceHandle; }
    public long getHwnd() { return hwnd; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean hasSurface() { return surfaceHandle != 0; }

    public void close() {
        if (surfaceHandle != 0) {
            Dx12Native.dx12DestroySurface(surfaceHandle);
            System.out.printf("  Surface destroyed (handle=0x%016x)%n", surfaceHandle);
            surfaceHandle = 0L;
        }
    }
}
