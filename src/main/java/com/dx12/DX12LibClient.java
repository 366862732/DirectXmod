package com.dx12;

public class DX12LibClient {

    public static native boolean nativeInit(long hwnd, int width, int height);
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize(int width, int height);

    /** Sync clear color from glClearColor hook */
    public static native void nativeSetClearColor(float r, float g, float b, float a);

    /** Sync current color from glColor4f hook */
    public static native void nativeSetGlColor(float r, float g, float b, float a);

    public static native boolean nativeIsInitialized();
    public static native void nativeShowDebugWindow(boolean show);

    /** Upload raw RGBA pixels to D3D12 texture for display */
    public static native void nativeUploadPixels(byte[] rgba, int width, int height);

    /** Record vertices for GL→D3D12 immediate-mode draw.
     *  Float array packed as [x,y,z,r,g,b,a] per vertex. */
    public static native void nativeRecordVertices(float[] vertices, int count);

    /** Set D3D12 primitive topology from GL enum (0=undefined, 1=point, 4=triangle...) */
    public static native void nativeSetPrimitiveTopology(int glPrimitiveMode);
}
