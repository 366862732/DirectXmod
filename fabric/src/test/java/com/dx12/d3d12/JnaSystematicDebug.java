package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;

/**
 * Systematic diagnostic: compare JNA Library proxy vs manual VTable access.
 */
public class JnaSystematicDebug {
    // IID_IDXGIFactory1
    private static final byte[] IID_IDXGIFactory1 = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);
    // IID_IDXGIObject (parent of all DXGI objects)
    private static final byte[] IID_IDXGIObject = makeGuid(0x077E1165L, 0xCDA7, 0x43CB,
        (byte)0xB1,(byte)0xAF,(byte)0xBC,(byte)0xD6,(byte)0xBB,(byte)0xA7,(byte)0x4B,(byte)0x32);

    // =========================================================================
    // Approach 1: JNA Library proxy (what JNA generates internally)
    // =========================================================================
    interface IUnknown extends Library {
        int QueryInterface(Pointer riid, PointerByReference ppv);
        long AddRef();
        long Release();
    }

    interface IDXGIFactory1_Proxy extends IUnknown {
        int EnumAdapters(int idx, PointerByReference pAdapter);
        int CheckInterfaceSupport(long nDllVersion, PointerByReference pUnk, PointerByReference pMonitorRefreshInterval);
    }

    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    // =========================================================================
    public static void main(String[] args) throws Exception {
        System.out.println("=== Systematic Diagnostic ===\n");

        // ---- Test A: Create factory via JNA Library proxy ----
        System.out.println("--- [A] JNA Library Proxy approach ---");
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory1), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));
        Pointer factoryPtr = ref.getValue();
        System.out.printf("factoryPtr = %s (native=0x%s)%n",
            factoryPtr, Long.toHexString(Pointer.nativeValue(factoryPtr)));

        // Try to cast to IUnknown proxy
        System.out.println("\n  Calling Release() via IUnknown proxy:");
        try {
            IUnknown unknown = (IUnknown) factoryPtr;
            long refcount = unknown.Release();
            System.out.printf("  Release() returned refcount=%d (proxy worked!)%n", refcount);
        } catch (Throwable t) {
            System.out.printf("  IUnknown proxy FAILED: %s%n", t.getMessage());
            t.printStackTrace(System.out);
        }

        // ---- Test B: Read raw bytes around the pointer ----
        System.out.println("\n--- [B] Raw memory scan ---");
        if (factoryPtr != null) {
            long addr = Pointer.nativeValue(factoryPtr);
            System.out.printf("Pointer address: 0x%016x%n", addr);

            // Read 256 bytes as raw data
            byte[] raw = factoryPtr.getByteArray(0, 256);
            System.out.println("First 256 bytes (hex + ascii):");
            for (int i = 0; i < 256; i += 16) {
                StringBuilder sb = new StringBuilder();
                StringBuilder ascii = new StringBuilder();
                for (int j = 0; j < 16; j++) {
                    int b = raw[i + j] & 0xFF;
                    sb.append(String.format("%02x ", b));
                    ascii.append(b >= 32 && b < 127 ? (char) b : '.');
                }
                System.out.printf("  %04x: %s  %s%n", i, sb.toString().trim(), ascii);
            }
        }

        // ---- Test C: Use JNA Library proxy to call methods directly ----
        System.out.println("\n--- [C] IDXGIFactory1 proxy with EnumAdapters ---");
        try {
            // This won't work directly - JNA proxy requires special setup
            // Let's try the low-level approach instead
            PointerByReference adapterRef = new PointerByReference();
            int ehrr = invokeManual(factoryPtr, 7 /* EnumAdapters */, 0, adapterRef);
            System.out.printf("Manual EnumAdapters(0): hr=0x%s, adapter=%s%n",
                Integer.toHexString(ehrr), adapterRef.getValue());
        } catch (Throwable t) {
            System.out.printf("Manual EnumAdapters THREW: %s%n", t.getMessage());
        }

        // ---- Test D: Try with dxgi.dll loaded differently ----
        System.out.println("\n--- [D] Direct LoadLibrary approach ---");
        try {
            Pointer factoryPtr2 = createFactoryManual();
            if (factoryPtr2 != null) {
                System.out.printf("Manual factory: %s (native=0x%s)%n",
                    factoryPtr2, Long.toHexString(Pointer.nativeValue(factoryPtr2)));
                // Try to read vtable
                for (int i = 0; i < 5; i++) {
                    long entry = factoryPtr2.getLong(i * 8);
                    System.out.printf("  [%d] = 0x%016x%n", i, entry);
                }
            }
        } catch (Throwable t) {
            System.out.printf("Manual approach THREW: %s%n", t.getMessage());
        }

        System.out.println("\nDone.");
    }

    static int invokeManual(Pointer object, int vtableIndex, Object... args) throws Exception {
        long fnAddr = object.getLong(vtableIndex * 8);
        if (fnAddr == 0) throw new RuntimeException("Null vtable entry at " + vtableIndex);
        Function fn = Function.getFunction(new Pointer(fnAddr));
        return fn.invokeInt(args);
    }

    static Pointer createFactoryManual() throws Exception {
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory1), ref);
        if (hr != 0) return null;
        return ref.getValue();
    }

    static byte[] makeGuid(long a, long b, long c, byte d0, byte d1, byte d2, byte d3, byte d4, byte d5, byte d6, byte d7) {
        byte[] g = new byte[16];
        g[0]=(byte)a;      g[1]=(byte)(a>>8);   g[2]=(byte)(a>>16);  g[3]=(byte)(a>>24);
        g[4]=(byte)b;      g[5]=(byte)(b>>8);
        g[6]=(byte)c;      g[7]=(byte)(c>>8);
        g[8]=d0; g[9]=d1; g[10]=d2; g[11]=d3; g[12]=d4; g[13]=d5; g[14]=d6; g[15]=d7;
        return g;
    }

    static Pointer writeGuidBytes(byte[] bytes) {
        Memory m = new Memory(bytes.length);
        m.write(0, bytes, 0, bytes.length);
        return m;
    }
}
