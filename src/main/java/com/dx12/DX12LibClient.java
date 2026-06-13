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

    /** Record vertices for GL→D3D12 draw. Float[7]: [x,y,z,r,g,b,a] per vertex. */
    public static native void nativeRecordVertices(float[] vertices, int count);

    /** Record vertices WITH UV coords. Float[9]: [x,y,z,r,g,b,a,u,v] per vertex. */
    public static native void nativeRecordVerticesUV(float[] vertices, int count);

    /** Set D3D12 primitive topology from GL enum (0=undefined, 1=point, 4=triangle...) */
    public static native void nativeSetPrimitiveTopology(int glPrimitiveMode);

    /** Set active D3D12 texture SRV by GL texture ID */
    public static native void nativeSetTexture(int glTextureId);

    /** Set texture ID for the next draw chunk (per-chunk binding) */
    public static native void nativeSetDrawTexture(int glTextureId);

    /** Upload RGBA texture pixels to D3D12 SRV, keyed by GL texture ID */
    public static native void nativeUploadTextureEx(byte[] rgba, int width, int height, int glTextureId);

    /** Set GL state bits for PSO variant selection.
     *  enableBits/disableBits: GLB_BLEND(1), GLB_DEPTH(2), GLB_CULL(4), GLB_DEPTH_WRITE(8) */
    public static native void nativeSetGlState(int enableBits, int disableBits);

    /** Sync viewport from glViewport(x,y,w,h) */
    public static native void nativeSetViewport(int x, int y, int w, int h);

    /** Sync blend func from glBlendFunc(sfactor, dfactor) */
    public static native void nativeSetBlendFunc(int sfactor, int dfactor);

    /** Sync depth mask from glDepthMask(flag) */
    public static native void nativeSetDepthMask(boolean write);

    /** Upload MVP transform (16 floats, column-major) */
    public static native void nativeSetMvp(float[] mvp);

    /** D3D12 window dimensions for pixel→clip transform */
    public static native int nativeGetWindowWidth();
    public static native int nativeGetWindowHeight();
}
