package com.xgdt.dx11.dx11;

/**
 * A Java wrapper that owns one opaque native handle in {@code dx11_mc.dll}.
 *
 * <p>The device keeps a registry of live resources so a hard shutdown (for
 * example a crash during world load) still releases every native struct before
 * the D3D11 device is destroyed.</p>
 */
interface Dx11Resource {
    long nativeHandle();

    /** Releases the native handle exactly once. */
    void destroyNative();
}
