package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

/**
 * Test: Use simple CreateDXGIFactory + IID_IDXGIFactory (no version suffix).
 * This is the most basic DXGI factory creation, which might return a simpler object.
 */
public class SimpleFactoryTest {
    // IID_IDXGIFactory (not 1, not 4 - the base interface)
    private static final byte[] IID_IDXGIFactory = makeGuid(0x7B7E9361L, 0xFC9E, 0x4FD1,
        (byte)0xA7,(byte)0x7E,(byte)0xEC,(byte)0x27,(byte)0x5B,(byte)0xF0,(byte)0x10,(byte)0x78);
    // IID_IDXGIFactory1
    private static final byte[] IID_IDXGIFactory1 = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);

    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory(Pointer riid, PointerByReference ppFactory);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
        int CreateDXGIFactory2(int Flags, Pointer riid, PointerByReference ppFactory);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Simple Factory Test ===\n");

        // Test 1: CreateDXGIFactory with IID_IDXGIFactory
        System.out.println("--- Test 1: CreateDXGIFactory + IID_IDXGIFactory ---");
        testFactory("CreateDXGIFactory",
            DxgiLib.INSTANCE::CreateDXGIFactory,
            IID_IDXGIFactory);

        // Test 2: CreateDXGIFactory with IID_IDXGIFactory1
        System.out.println("\n--- Test 2: CreateDXGIFactory + IID_IDXGIFactory1 ---");
        testFactory("CreateDXGIFactory(IID_IDXGIFactory1)",
            (Pointer riid, PointerByReference ref) -> DxgiLib.INSTANCE.CreateDXGIFactory(riid, ref),
            IID_IDXGIFactory1);

        // Test 3: CreateDXGIFactory1 with IID_IDXGIFactory1
        System.out.println("\n--- Test 3: CreateDXGIFactory1 + IID_IDXGIFactory1 ---");
        testFactory("CreateDXGIFactory1(IID_IDXGIFactory1)",
            DxgiLib.INSTANCE::CreateDXGIFactory1,
            IID_IDXGIFactory1);

        // Test 4: CreateDXGIFactory1 with IID_IDXGIFactory4
        System.out.println("\n--- Test 4: CreateDXGIFactory1 + IID_IDXGIFactory4 ---");
        testFactory("CreateDXGIFactory1(IID_IDXGIFactory4)",
            DxgiLib.INSTANCE::CreateDXGIFactory1,
            makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
                (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A));

        // Test 5: CreateDXGIFactory2 with IID_IDXGIFactory4
        System.out.println("\n--- Test 5: CreateDXGIFactory2 + IID_IDXGIFactory4 ---");
        testFactory("CreateDXGIFactory2(IID_IDXGIFactory4)",
            (Pointer riid, PointerByReference ref) -> DxgiLib.INSTANCE.CreateDXGIFactory2(0, riid, ref),
            makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
                (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A));

        System.out.println("\nDone.");
    }

    interface FactoryCreator {
        int create(Pointer riid, PointerByReference ref);
    }

    static void testFactory(String name, FactoryCreator creator, byte[] iid) {
        try {
            PointerByReference ref = new PointerByReference();
            Memory iidMem = new Memory(16);
            iidMem.write(0, iid, 0, 16);
            int hr = creator.create(iidMem, ref);
            System.out.printf("  %s: hr=0x%s, ptr=%s%n", name, Integer.toHexString(hr), ref.getValue());
            if (hr != 0 || ref.getValue() == null) {
                System.out.printf("  FAILED%n");
                return;
            }
            Pointer factory = ref.getValue();
            long fnAddr0 = factory.getLong(0);
            long fnAddr1 = factory.getLong(8);
            long fnAddr2 = factory.getLong(16);
            System.out.printf("  vtable[0]=0x%016x [1]=0x%016x [2]=0x%016x%n", fnAddr0, fnAddr1, fnAddr2);

            // Try calling Release
            if (fnAddr2 != 0) {
                try {
                    Function fn = Function.getFunction(new Pointer(fnAddr2));
                    long refcount = fn.invokeLong(new Object[]{factory});
                    System.out.printf("  Release() succeeded: refcount=%d%n", refcount);
                } catch (Throwable t) {
                    System.out.printf("  Release() FAILED: %s%n", t.getMessage());
                }
            }

            // Try calling EnumAdapters at index 6
            if (fnAddr0 != 0) {
                try {
                    // First try QI to get an IDXGIFactory1
                    PointerByReference qiRef = new PointerByReference();
                    Function qiFn = Function.getFunction(new Pointer(fnAddr0));
                    Memory iidMem2 = new Memory(16);
                    iidMem2.write(0, IID_IDXGIFactory1, 0, 16);
                    int qhr = qiFn.invokeInt(new Object[]{factory, iidMem2, qiRef});
                    System.out.printf("  QI for IDXGIFactory1: hr=0x%s, ptr=%s%n",
                        Integer.toHexString(qhr), qiRef.getValue());
                    if (qhr == 0 && qiRef.getValue() != null) {
                        Pointer realFactory = qiRef.getValue();
                        long rAddr = realFactory.getLong(16);
                        System.out.printf("  Real factory Release=0x%016x%n", rAddr);
                        if (rAddr != 0) {
                            Function rFn = Function.getFunction(new Pointer(rAddr));
                            long rc = rFn.invokeLong(new Object[]{realFactory});
                            System.out.printf("  Real factory Release() = %d%n", rc);
                        }
                    }
                } catch (Throwable t) {
                    System.out.printf("  QI FAILED: %s%n", t.getMessage());
                }
            }
        } catch (Throwable t) {
            System.out.printf("  THREW: %s%n", t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    static byte[] makeGuid(long a, long b, long c, byte d0, byte d1, byte d2, byte d3, byte d4, byte d5, byte d6, byte d7) {
        byte[] g = new byte[16];
        g[0]=(byte)a;      g[1]=(byte)(a>>8);   g[2]=(byte)(a>>16);  g[3]=(byte)(a>>24);
        g[4]=(byte)b;      g[5]=(byte)(b>>8);
        g[6]=(byte)c;      g[7]=(byte)(c>>8);
        g[8]=d0; g[9]=d1; g[10]=d2; g[11]=d3; g[12]=d4; g[13]=d5; g[14]=d6; g[15]=d7;
        return g;
    }
}
