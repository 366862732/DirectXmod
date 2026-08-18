package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java D3D12 device factory via JNA + direct VTable dispatch.
 *
 * Key design decisions:
 * - CreateDXGIFactory1 returns a raw Pointer (not a JNA proxy). We dispatch
 *   COM methods by reading the VTable manually and invoking via JNA Function.
 * - Java 26 requires --enable-native-access=ALL-UNNAMED for JNA Function calls.
 * - Release() may be NULL on some systems (WinRT-style factory wrapper).
 *   In that case we skip explicit Release and rely on OS cleanup.
 */
public class Dx12Device implements AutoCloseable {

    // Feature levels
    public static final int D3D_FEATURE_LEVEL_12_0 = 0xC000;
    public static final int D3D_FEATURE_LEVEL_11_1 = 0xB000;
    public static final int D3D_FEATURE_LEVEL_11_0 = 0xA000;

    private static final int S_OK = 0x00000000;
    private static final int DXGI_ERROR_NOT_FOUND = 0x887A0003;
    private static final int E_FAIL = 0x80004005;

    // IID definitions (little-endian bytes)
    private static final byte[] IID_ID3D12Device  = makeGuid(0x1B16AC2DL, 0x24, 0x42,
        (byte)0x33,(byte)0xF1,(byte)0x44,(byte)0xD3,(byte)0x07,(byte)0x04,(byte)0xE4,(byte)0x84);
    private static final byte[] IID_IDXGIFactory4 = makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
        (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A);
    private static final byte[] IID_IDXGIAdapter4 = makeGuid(0x7515E5AAL, 0x41B5, 0x476E,
        (byte)0xAD,(byte)0xE8,(byte)0x88,(byte)0x0D,(byte)0x9A,(byte)0x9F,(byte)0xC8,(byte)0x42);

    // =========================================================================
    // JNA native library interfaces
    // =========================================================================

    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    interface D3D12Lib extends Library {
        D3D12Lib INSTANCE = Native.load("d3d12", D3D12Lib.class);
        int D3D12CreateDevice(Pointer pAdapter, int FeatureLevel, Pointer riid, PointerByReference ppDevice);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    private volatile Pointer devicePointer;

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

    public static List<Dx12AdapterInfo> enumerateAdapters() {
        List<Dx12AdapterInfo> result = new ArrayList<>();
        Pointer factory = null;
        try {
            factory = createFactory();
            System.out.printf("  Factory ptr=0x%s%n", Long.toHexString(Pointer.nativeValue(factory)));
            result = enumerateAdaptersFromFactory(factory);
        } catch (Exception e) {
            System.err.printf("Failed to enumerate DXGI adapters: %s%n", e.getMessage());
            e.printStackTrace();
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
        // Don't call Release here — let D3D12CreateDevice handle lifecycle via the factory
        devicePointer = null;
        System.out.println("D3D12 device released (Java side)");
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private Dx12Device(Pointer ptr) { this.devicePointer = ptr; }

    static Pointer createFactory() throws Dx12Exception {
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory4), ref);
        System.out.printf("  CreateDXGIFactory1 hr=0x%s ref=%s%n", Integer.toHexString(hr), ref.getValue());
        if (hr != S_OK || ref.getValue() == null) {
            throw new Dx12Exception("CreateDXGIFactory1 failed, HRESULT=0x" + Integer.toHexString(hr), hr);
        }
        return ref.getValue();
    }

    /**
     * Enumerate adapters using VTable dispatch.
     * VTable layout for IDXGIFactory4:
     *   [0] = QueryInterface
     *   [1] = AddRef
     *   [2] = Release
     *   [3] = SetWindowAssociation
     *   [4] = CreateSwapChain
     *   [5] = CreateSurface  (not in IDXGIFactory4, skip)
     *   [6] = GetAdapter
     */
    static List<Dx12AdapterInfo> enumerateAdaptersFromFactory(Pointer factory) throws Exception {
        List<Dx12AdapterInfo> result = new ArrayList<>();
        for (int i = 0; ; i++) {
            Pointer adapter = enumAdapter(factory, i);
            if (adapter == null) break;
            Dx12AdapterInfo info = readAdapterInfo(adapter);
            if (info != null) result.add(info);
            safeRelease(adapter, 2); // Release index for IDXGIAdapter
        }
        return result;
    }

    static Pointer enumAdapter(Pointer factory, int index) throws Exception {
        // VTable[6] = GetAdapter for IDXGIFactory4
        long fnAddr = factory.getLong(6 * 8);
        System.out.printf("    GetAdapter vtable[6]=0x%016x%n", fnAddr);
        if (fnAddr == 0) return null;
        Function fn = Function.getFunction(new Pointer(fnAddr));
        PointerByReference ref = new PointerByReference();
        // JNA 5.17 invokeInt takes Object[] (not varargs)
        int hr = fn.invokeInt(new Object[]{factory, index, ref});
        System.out.printf("    GetAdapter(%d): hr=0x%s adapter=%s%n",
            index, Integer.toHexString(hr), ref.getValue());
        if (hr == S_OK && ref.getValue() != null) return ref.getValue();
        return null;
    }

    /**
     * Read adapter info via GetDesc5 (IDXGIAdapter4, vtable[9]).
     * DXGI_ADAPTER_DESC5 layout (first 328 bytes):
     *   [0..7]   Reserved (8 bytes)
     *   [8..511] AdapterName (128 WCHAR = 256 bytes)
     *   [280]    VendorId (4 bytes)
     *   [284]    DeviceId (4 bytes)
     *   [296]    DedicatedVideoMemory (8 bytes)
     *   [304]    DedicatedSystemMemory (8 bytes)
     *   [312]    SharedSystemMemory (8 bytes)
     */
    static Dx12AdapterInfo readAdapterInfo(Pointer adapter) throws Exception {
        // First try GetDesc5 at vtable[9] (IDXGIAdapter4)
        long fnAddr = adapter.getLong(9 * 8);
        System.out.printf("    GetDesc5 vtable[9]=0x%016x%n", fnAddr);
        if (fnAddr == 0) {
            // Fallback to GetDesc at vtable[7] (IDXGIAdapter)
            fnAddr = adapter.getLong(7 * 8);
            System.out.printf("    GetDesc fallback vtable[7]=0x%016x%n", fnAddr);
        }
        if (fnAddr == 0) {
            System.err.println("    No GetDesc/GetDesc5 found, skipping");
            return null;
        }
        Function fn = Function.getFunction(new Pointer(fnAddr));
        Memory desc = new Memory(328);
        int hr = fn.invokeInt(adapter, desc);
        System.out.printf("    GetDesc/hr=0x%s%n", Integer.toHexString(hr));
        if (hr != S_OK) return null;

        // Read name (WCHAR array at offset 8 for DESC5, offset 0 for legacy DESC)
        // DXGI_ADAPTER_DESC5: Name starts at offset 8 (after Reserved[8])
        // DXGI_ADAPTER_DESC:  Name starts at offset 0
        // Heuristic: if offset 0 has zeros, it's DESC5; otherwise DESC
        byte[] rawName = desc.getByteArray(8, 256);
        String name = new String(rawName, StandardCharsets.UTF_16LE).replaceFirst("\u0000+$", "");
        if (name.isEmpty()) {
            // Try offset 0 (legacy DXGI_ADAPTER_DESC)
            rawName = desc.getByteArray(0, 256);
            name = new String(rawName, StandardCharsets.UTF_16LE).replaceFirst("\u0000+$", "");
        }

        long luid = desc.getLong(0) | (desc.getLong(8) << 32);
        int vid = desc.getInt(280);
        int did = desc.getInt(284);
        long dvm = desc.getLong(296);
        long dsm = desc.getLong(304);
        long ssm = desc.getLong(312);

        System.out.printf("    Adapter: %s | VID=0x%s DID=0x%s VRAM=%dGiB%n",
            name, Integer.toHexString(vid), Integer.toHexString(did), dvm / (1024L * 1024 * 1024));
        return Dx12AdapterInfo.of(name, luid, vid, did, dvm, dsm, ssm, 16384);
    }

    static void safeRelease(Pointer obj, int releaseIndex) {
        if (obj == null) return;
        try {
            long fnAddr = obj.getLong(releaseIndex * 8);
            if (fnAddr == 0) {
                System.out.printf("    Release vtable[%d] is NULL, skipping (OS will clean up)%n", releaseIndex);
                return;
            }
            Function fn = Function.getFunction(new Pointer(fnAddr));
            fn.invokeVoid(new Object[]{obj});
            System.out.printf("    Released object at 0x%s%n", Long.toHexString(Pointer.nativeValue(obj)));
        } catch (Exception e) {
            System.out.printf("    Release failed (0x%s), skipping%n", e.getMessage().toLowerCase());
        }
    }

    private static Pointer getFirstAdapter() {
        Pointer factory = null;
        try {
            factory = createFactory();
            return enumAdapter(factory, 0);
        } catch (Exception e) {
            System.err.printf("Failed to create DXGI factory: %s%n", e.getMessage());
            return null;
        } finally {
            safeRelease(factory, 2);
        }
    }

    private static Dx12Device tryCreateDevice(Pointer adapter, int featureLevel) {
        PointerByReference ref = new PointerByReference();
        Pointer iidPtr = writeGuidBytes(IID_ID3D12Device);
        int hr = D3D12Lib.INSTANCE.D3D12CreateDevice(adapter, featureLevel, iidPtr, ref);
        System.out.printf("  D3D12CreateDevice FL=0x%s hr=0x%s ref=%s%n",
            Integer.toHexString(featureLevel), Integer.toHexString(hr), ref.getValue());
        if (hr == S_OK && ref.getValue() != null) {
            System.out.printf("  D3D12 device created (feature level 0x%s)%n", Integer.toHexString(featureLevel));
            return new Dx12Device(ref.getValue());
        }
        return null;
    }

    // =========================================================================
    // GUID helpers
    // =========================================================================

    private static byte[] makeGuid(long a, long b, long c, byte d0, byte d1, byte d2, byte d3,
                                    byte d4, byte d5, byte d6, byte d7) {
        byte[] g = new byte[16];
        g[0]  = (byte)(a        & 0xFF);
        g[1]  = (byte)((a >>  8) & 0xFF);
        g[2]  = (byte)((a >> 16) & 0xFF);
        g[3]  = (byte)((a >> 24) & 0xFF);
        g[4]  = (byte)(b        & 0xFF);
        g[5]  = (byte)((b >>  8) & 0xFF);
        g[6]  = (byte)(c        & 0xFF);
        g[7]  = (byte)((c >>  8) & 0xFF);
        g[8]  = d0; g[9]  = d1; g[10] = d2; g[11] = d3;
        g[12] = d4; g[13] = d5; g[14] = d6; g[15] = d7;
        return g;
    }

    private static Pointer writeGuidBytes(byte[] guid) {
        Memory m = new Memory(16);
        m.write(0, guid, 0, 16);
        return m;
    }
}
