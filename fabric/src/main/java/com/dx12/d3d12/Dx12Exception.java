package com.dx12.d3d12;

/**
 * Exception thrown when a D3D12 operation fails.
 *
 * Carries the {@code HRESULT} value so callers can inspect the exact COM error code.
 */
public class Dx12Exception extends RuntimeException {

    private final int hresult;

    public Dx12Exception(String message, int hresult) {
        super(message);
        this.hresult = hresult;
    }

    public Dx12Exception(String message, int hresult, Throwable cause) {
        super(message, cause);
        this.hresult = hresult;
    }

    /** Returns the raw HRESULT returned by the D3D12/DXGI call. */
    public int hresult() {
        return hresult;
    }
}
