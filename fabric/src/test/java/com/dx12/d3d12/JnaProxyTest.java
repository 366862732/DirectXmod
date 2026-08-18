package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

/**
 * Test: Use JNA's automatic proxy generation for dxgi.dll.
 * Define IDXGIFactory1 as a JNA Library interface, call CreateDXGIFactory1
 * through a direct LoadLibrary approach, then use the proxy for method calls.
 */
public class JnaProxyTest {

    // IID_IDXGIFactory1 = {770AAE78-F26F-4DBA-A829-253C83D1B387}
    private static final byte[] IID_IDXGIFactory1 = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);

    // =========================================================================
    // JNA interfaces
    // =========================================================================

    /** DXGI factory interface — JNA will auto-generate proxy */
    public interface IDXGIFactory1 extends Library {
        /** IUnknown */
        int QueryInterface(Pointer riid, PointerByReference ppv);
        long AddRef();
        long Release();
        /** IDXGIFactory1 */
        int EnumAdapters(int iAdapter, PointerByReference ppAdapter);
        int CheckInterfaceSupport(long nDllVersion, Pointer pNonFreeableInterface,
                                  PointerByReference pTimestamp);
    }

    /** dxgi.dll raw functions */
    interface DxgiNativeLib extends Library {
        DxgiNativeLib INSTANCE = Native.load("dxgi", DxgiNativeLib.class);
        /**
         * Creates a DXGI factory object.
         * Note: ppFactory is an out parameter — JNA will write the result pointer.
         */
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    // =========================================================================
    public static void main(String[] args) throws Exception {
        System.out.println("=== JNA Proxy Test ===\n");

        // Approach 1: Call CreateDXGIFactory1, get raw Pointer, try to use as proxy
        System.out.println("--- Approach 1: raw Pointer from JNA ---");
        PointerByReference ref = new PointerByReference();
        int hr = DxgiNativeLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory1), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));
        System.out.printf("  ref.getValue() = %s (native=0x%016x)%n",
            ref.getValue(), Pointer.nativeValue(ref.getValue()));

        // Try to use as Library proxy
        System.out.println("\n--- Approach 2: Cast to IDXGIFactory1 proxy ---");
        try {
            // JNA can create a proxy from a Pointer by using Library.newInstance
            // or by casting through the proxy creation mechanism
            IDXGIFactory1 factory = (IDXGIFactory1) ref.getValue();
            System.out.println("Cast succeeded (unexpected?)");
        } catch (ClassCastException e) {
            System.out.printf("Cast failed (expected): %s%n", e.getMessage());
        }

        // Approach 3: Use Native.getFunction() to call methods directly
        System.out.println("\n--- Approach 3: Get function from dxgi.dll by name ---");
        // Can't easily do this for COM vtable methods...

        // Approach 4: Read the pointer and find the real COM object inside
        System.out.println("\n--- Approach 4: Inspect pointer contents ---");
        Pointer p = ref.getValue();
        long nativeAddr = Pointer.nativeValue(p);
        System.out.printf("Pointer native address: 0x%016x%n", nativeAddr);

        // Try reading the vtable at various offsets
        System.out.println("\nVTable candidates:");
        // Standard COM: [0]=QI, [1]=AddRef, [2]=Release
        long qiraw = p.getLong(0);
        long arraw = p.getLong(8);
        long relraw = p.getLong(16);
        System.out.printf("  [0] QI  = 0x%016x%n", qiraw);
        System.out.printf("  [1] AddRef = 0x%016x%n", arraw);
        System.out.printf("  [2] Release = 0x%016x%n", relraw);

        // Also try reading at offset 8 from native address (skip QI+AddRef = 16 bytes)
        System.out.println("\n  If this is a JNA proxy, the real COM pointer might be at a fixed offset:");
        // JNA proxy objects sometimes store the real pointer at offset 0 or offset 8
        for (int skipOffset : new int[]{0, 8, 16, 24, 32}) {
            long candidate = p.getLong(skipOffset);
            if (candidate != 0) {
                System.out.printf("  offset+%d: 0x%016x (possible COM ptr?)%n", skipOffset, candidate);
                // Read vtable of candidate
                Pointer cand = new Pointer(candidate);
                System.out.printf("    cand[0]=QI=0x%016x cand[1]=AddRef=0x%016x cand[2]=Release=0x%016x%n",
                    cand.getLong(0), cand.getLong(8), cand.getLong(16));
            }
        }

        // Approach 5: Try calling QI via Function.getFunction with the QI address
        System.out.println("\n--- Approach 5: Call QI directly via Function ---");
        try {
            Pointer qiFnPtr = new Pointer(qiraw);
            Function qiFn = Function.getFunction(qiFnPtr);
            PointerByReference qiRef = new PointerByReference();
            Pointer iidMem = writeGuidBytes(IID_IDXGIFactory1);
            // __stdcall thiscall: first arg is 'this', then riid, then ppv
            Object result = qiFn.invokeInt(new Object[]{p, iidMem, qiRef});
            int qiHr = ((Number) result).intValue();
            System.out.printf("QI(hr=0x%s) ppv=%s%n", Integer.toHexString(qiHr), qiRef.getValue());
            if (qiHr == 0 && qiRef.getValue() != null) {
                Pointer newPtr = qiRef.getValue();
                System.out.printf("  New ptr native=0x%016x%n", Pointer.nativeValue(newPtr));
                // Try to use as proxy
                try {
                    IDXGIFactory1 factory = (IDXGIFactory1) newPtr;
                    System.out.println("  Cast to IDXGIFactory1: SUCCESS!");
                } catch (ClassCastException e2) {
                    System.out.printf("  Cast to IDXGIFactory1: FAILED - %s%n", e2.getMessage());
                }
            }
        } catch (Throwable t) {
            System.out.printf("QI threw: %s%n", t.getMessage());
            t.printStackTrace(System.out);
        }

        // Approach 6: Direct Library invocation via JNA's proxy mechanism
        System.out.println("\n--- Approach 6: Use Native library handle ---");
        try {
            // Get the dxgi.dll handle
            NativeLibrary dxgiLib = NativeLibrary.getInstance("dxgi");
            // Find the CreateDXGIFactory1 function
            com.sun.jna.Function fn = dxgiLib.getFunction("CreateDXGIFactory1");
            // Call it manually
            Memory iidMem2 = new Memory(16);
            iidMem2.write(0, IID_IDXGIFactory1, 0, 16);
            Memory outPtrMem = new Memory(8); // space for the output pointer
            int callResult = fn.invokeInt(new Object[]{iidMem2, outPtrMem});
            System.out.printf("CreateDXGIFactory1 (manual) hr=0x%s%n", Integer.toHexString(callResult));
            long outPtrAddr = outPtrMem.getLong(0);
            System.out.printf("  Output ptr addr: 0x%016x%n", outPtrAddr);
            if (outPtrAddr != 0) {
                Pointer outPtr = new Pointer(outPtrAddr);
                System.out.println("  VTable of output ptr:");
                for (int i = 0; i < 5; i++) {
                    System.out.printf("    [%d] = 0x%016x%n", i, outPtr.getLong(i * 8));
                }
            }
        } catch (Throwable t) {
            System.out.printf("Manual invoke threw: %s%n", t.getMessage());
            t.printStackTrace(System.out);
        }

        System.out.println("\nDone.");
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
