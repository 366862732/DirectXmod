package com.dx12.d3d12;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java D3D12 device factory using JNA (com.sun.jna).
 *
 * <p>This replaces the old JNI-based {@code Dx12Native.dx12CreateDevice()} with
 * a fully managed Java implementation.  The native DLLs (d3d12.dll, dxgi.dll)
 * are loaded on demand via JNA's native library resolver — no pre-built native
 * library is required.</p>
 *
 * <p><b>Step 1</b> of the JNI → pure-Java migration: device creation + adapter
 * enumeration only.  Resource creation, command encoding, and swapchain will
 * follow in subsequent steps.</p>
 */
public class Dx12Device implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Dx12Device.class);

    /** Feature level 12_0 (D3D_FEATURE_LEVEL_12_0) */
    public static final int D3D_FEATURE_LEVEL_12_0 = 0xC000;
    /** Feature level 11_1 */
    public static final int D3D_FEATURE_LEVEL_11_1 = 0xB000;
    /** Feature level 11_0 */
    public static final int D3D_FEATURE_LEVEL_11_0 = 0xA000;

    private static final int S_OK = 0x00000000;
    private static final int DXGI_ERROR_NOT_FOUND = 0x887A0003;
    private static final int E_FAIL = 0x80004005;

    // --- IIDs (byte[16], little-endian GUID layout) ---
    /** IID_ID3D12Device */
    private static final byte[] IID_ID3D12Device   = makeGuid(0x1B16AC2DL, 0x24, 0x42,
        (byte)0x33,(byte)0xF1,(byte)0x44,(byte)0xD3,(byte)0x07,(byte)0x04,(byte)0xE4,(byte)0x84);
    /** IID_IDXGIFactory1 */
    private static final byte[] IID_IDXGIFactory1  = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);

    // =========================================================================
    // JNA interface for D3D12 (d3d12.dll)
    // =========================================================================
    interface D3D12Lib extends Library {
        D3D12Lib INSTANCE = Native.load("d3d12", D3D12Lib.class);
        /** D3D12CreateDevice(pAdapter, FeatureLevel, riid, ppDevice) → HRESULT */
        int D3D12CreateDevice(Pointer pAdapter, int FeatureLevel, Pointer riid, PointerByReference ppDevice);
    }

    // =========================================================================
    // JNA interface for DXGI (dxgi.dll)
    // =========================================================================
    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        /** CreateDXGIFactory1(riid, ppFactory) → HRESULT */
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /** Raw ID3D12Device* handle. Null after close(). */
    private volatile Pointer devicePointer;

    /**
     * Creates a D3D12 device using the default (first) adapter.
     * Tries feature levels 12_0 → 11_1 → 11_0.
     */
    public static Dx12Device create() {
        Pointer adapter = getFirstAdapter();
        if (adapter == null) {
            throw new Dx12Exception("No D3D12 adapter found", DXGI_ERROR_NOT_FOUND);
        }
        int[] levels = { D3D_FEATURE_LEVEL_12_0, D3D_FEATURE_LEVEL_11_1, D3D_FEATURE_LEVEL_11_0 };
        for (int fl : levels) {
            Dx12Device dev = tryCreateDevice(adapter, fl);
            if (dev != null) return dev;
        }
        throw new Dx12Exception("D3D12CreateDevice failed (all feature levels rejected)", E_FAIL);
    }

    /** Returns all available adapters as {@link Dx12AdapterInfo}. */
    public static List<Dx12AdapterInfo> enumerateAdapters() {
        List<Dx12AdapterInfo> result = new ArrayList<>();
        try (ComPtr factory = createFactory()) {
            for (int i = 0; ; i++) {
                try (ComPtr adapter = enumAdapter(factory, i)) {
                    if (adapter == null) break;
                    Dx12AdapterInfo info = readAdapterInfo(adapter.get());
                    if (info != null) result.add(info);
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to enumerate DXGI adapters: {}", e.getMessage());
        }
        return result;
    }

    public Pointer getDevicePointer() {
        if (devicePointer == null) throw new IllegalStateException("D3D12Device is closed");
        return devicePointer;
    }

    public boolean isClosed() { return devicePointer == null; }

    @Override
    public void close() {
        if (devicePointer == null) return;
        devicePointer = null;
        LOG.info("D3D12 device released");
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private Dx12Device(Pointer ptr) { this.devicePointer = ptr; }

    private static Dx12Device tryCreateDevice(Pointer adapter, int featureLevel) {
        PointerByReference ref = new PointerByReference();
        Pointer iidPtr = writeGuidBytes(IID_ID3D12Device);
        int hr = D3D12Lib.INSTANCE.D3D12CreateDevice(adapter, featureLevel, iidPtr, ref);
        if (hr == S_OK && ref.getValue() != null) {
            LOG.info("D3D12 device created (feature level 0x{})", Integer.toHexString(featureLevel));
            return new Dx12Device(ref.getValue());
        }
        LOG.warn("D3D12CreateDevice failed at FL 0x{}, HRESULT=0x{}",
            Integer.toHexString(featureLevel), Integer.toHexString(hr));
        return null;
    }

    // =========================================================================
    // GUID helpers
    // =========================================================================

    private static byte[] makeGuid(long lo, int m1, int m2, byte b0, byte b1, byte b2, byte b3,
                                   byte b4, byte b5, byte b6, byte b7) {
        byte[] g = new byte[16];
        g[0]  = (byte)(lo        & 0xFF);
        g[1]  = (byte)((lo >>  8) & 0xFF);
        g[2]  = (byte)((lo >> 16) & 0xFF);
        g[3]  = (byte)((lo >> 24) & 0xFF);
        g[4]  = (byte)(m1        & 0xFF);
        g[5]  = (byte)((m1 >>  8) & 0xFF);
        g[6]  = (byte)(m2        & 0xFF);
        g[7]  = (byte)((m2 >>  8) & 0xFF);
        g[8]  = b0; g[9]  = b1; g[10] = b2; g[11] = b3;
        g[12] = b4; g[13] = b5; g[14] = b6; g[15] = b7;
        return g;
    }

    private static Pointer writeGuidBytes(byte[] guid) {
        com.sun.jna.Memory mem = new com.sun.jna.Memory(16);
        mem.write(0, guid, 0, 16);
        return mem;
    }

    // =========================================================================
    // Factory / adapter accessors
    // =========================================================================

    private static Pointer getFirstAdapter() {
        try (ComPtr factory = createFactory()) {
            try (ComPtr adapter = enumAdapter(factory, 0)) {
                return adapter == null ? null : adapter.get();
            }
        } catch (Exception e) {
            LOG.warn("Failed to create DXGI factory: {}", e.getMessage());
            return null;
        }
    }

    private static ComPtr createFactory() throws Dx12Exception {
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory1), ref);
        System.out.printf("CreateDXGIFactory1(hr=0x%s) ref=%s%n", Integer.toHexString(hr), ref.getValue());
        if (hr != S_OK || ref.getValue() == null) {
            throw new Dx12Exception("CreateDXGIFactory1 failed, HRESULT=0x" + Integer.toHexString(hr), hr);
        }
        Pointer ptr = ref.getValue();
        System.out.printf("  Raw pointer: 0x%s (nativeValue=%s)%n",
            Long.toHexString(Pointer.nativeValue(ptr)), ptr);
        // Dump first 20 vtable entries for debugging
        System.out.println("  First vtable entries:");
        for (int i = 0; i < 20; i++) {
            long entry = ptr.getLong(i * 8);
            System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(entry));
        }
        return new ComPtr(ptr);
    }

    private static ComPtr enumAdapter(ComPtr factory, int index) throws Exception {
        System.out.printf("enumAdapter: factory ptr=%s%n", factory.get());
        PointerByReference ref = new PointerByReference();
        try {
            int hr = invokeVTable(factory.get(), 7 /* EnumAdapters */, index, ref);
            System.out.printf("EnumAdapters(%d): hr=0x%s, out=%s%n", index, Integer.toHexString(hr), ref.getValue());
            if (hr == S_OK && ref.getValue() != null) {
                return new ComPtr(ref.getValue());
            }
        } catch (Exception e) {
            System.out.printf("EnumAdapters(%d) THREW: %s%n", index, e.getMessage());
            throw e;
        }
        return null;
    }

    private static Dx12AdapterInfo readAdapterInfo(Pointer adapter) throws Exception {
        // DXGI_ADAPTER_DESC5 layout (328 bytes, x64 little-endian):
        //   0-7:   LUID  (uint64)
        //   8-15:  LUID  (uint64)
        //   16-19: IsHardware (uint32)
        //   20-23: Flags (uint32)
        //   24-279: Description [WCHAR 128] (256 bytes)
        //   280-283: VendorId (uint32)
        //   284-287: DeviceId (uint32)
        //   288-291: SubSysId (uint32)
        //   292-295: Revision (uint32)
        //   296-303: DedicatedVideoMemory (uint64)
        //   304-311: DedicatedSystemMemory (uint64)
        //   312-319: SharedSystemMemory (uint64)
        //   320-327: AdapterLuid (uint64)
        com.sun.jna.Memory desc = new com.sun.jna.Memory(328);
        int hr = invokeVTable(adapter, 11 /* GetDesc4 */, desc);
        System.out.printf("GetDesc4(hr=0x%s)%n", Integer.toHexString(hr));
        if (hr != S_OK) return null;
        byte[] raw = desc.getByteArray(24, 256);
        String name = new String(raw, StandardCharsets.UTF_16LE).replaceFirst("\u0000+$", "");
        long luid = desc.getLong(0) | (desc.getLong(8) << 32);
        int vid = desc.getInt(280);
        int did = desc.getInt(284);
        long dvm = desc.getLong(296);
        long dsm = desc.getLong(304);
        long ssm = desc.getLong(312);
        return Dx12AdapterInfo.of(name, luid, vid, did, dvm, dsm, ssm, 16384);
    }

    // =========================================================================
    // COM VTable dispatch
    // =========================================================================

    /**
     * Invokes a COM interface method via its virtual table.
     *
     * <p>The vtable is stored at the object's memory address. Each entry is an
     * 8-byte function pointer (x64). This method reads the target function
     * pointer, wraps it in a {@link Function}, and invokes it.</p>
     *
     * @param object       the COM object pointer
     * @param vtableIndex  zero-based index into the vtable
     * @param args         method arguments (auto-marshalled by JNA)
     * @return the int HRESULT returned by the method
     */
    static int invokeVTable(Pointer object, int vtableIndex, Object... args) throws Exception {
        System.out.printf("invokeVTable: obj=%s vtableIndex=%d%n", object, vtableIndex);
        Pointer vtable = object;
        long fnAddr = vtable.getLong(vtableIndex * 8);
        System.out.printf("  vtable[%d] addr=0x%s%n", vtableIndex, Long.toHexString(fnAddr));
        if (fnAddr == 0) throw new Dx12Exception("Null vtable entry at index " + vtableIndex, 0x80004005);
        Function fn = Function.getFunction(new Pointer(fnAddr));
        Object result = fn.invoke(int.class, args);
        return ((Number) result).intValue();
    }

    // =========================================================================
    // Inner: ComPtr — auto-closeable COM pointer holder
    // =========================================================================

    /**
     * Holds a COM interface pointer and auto-Releases it on close.
     */
    static class ComPtr implements AutoCloseable {
        private final Pointer ptr;
        private boolean released;

        ComPtr(Pointer ptr) {
            System.out.printf("  ComPtr created: ptr=%s nativeValue=0x%s%n", ptr, Long.toHexString(Pointer.nativeValue(ptr)));
            this.ptr = ptr;
        }
        Pointer get() { return ptr; }

        @Override
        public void close() {
            if (released || ptr == null) return;
            released = true;
            try { invokeVTable(ptr, 2 /* Release */); } catch (Exception ignored) {}
        }
    }
}
