package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;

/**
 * Final diagnostic: understand why invokeInt fails on valid vtable entries.
 */
public class FinalDebug {
    private static final byte[] IID_IDXGIFactory4 = makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
        (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A);

    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Final Diagnostic ===\n");

        // Create factory
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory4), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));
        Pointer factory = ref.getValue();
        System.out.printf("factory = %s, native=0x%016x%n", factory, Pointer.nativeValue(factory));

        // Check if factory is a special JNA type
        System.out.printf("factory class: %s%n", factory.getClass().getName());
        System.out.printf("factory class super: %s%n", factory.getClass().getSuperclass());
        System.out.printf("factory class interfaces: ");
        for (Class<?> iface : factory.getClass().getInterfaces()) {
            System.out.printf("%s ", iface.getName());
        }
        System.out.println();

        // Try calling Release directly via Function
        long releaseAddr = factory.getLong(16);
        System.out.printf("\nRelease at [2] = 0x%016x%n", releaseAddr);

        // If Release is NULL, try calling QueryInterface to get a real COM pointer
        long qiAddr = factory.getLong(0);
        long addrefAddr = factory.getLong(8);
        System.out.printf("QI=0x%016x AddRef=0x%016x%n", qiAddr, addrefAddr);

        // Check what module these addresses belong to
        System.out.println("\n--- Checking address ranges ---");
        System.out.printf("QI addr: 0x%016x%n", qiAddr);
        System.out.printf("AddRef addr: 0x%016x%n", addrefAddr);
        System.out.printf("Release addr: 0x%016x%n", releaseAddr);

        // Try calling QI to get a DIFFERENT pointer (maybe a real COM object)
        System.out.println("\n--- Calling QI ---");
        try {
            // Use the raw function address directly
            Function qiFn = Function.getFunction(new Pointer(qiAddr));
            PointerByReference qiRef = new PointerByReference();
            Memory iidMem = new Memory(16);
            iidMem.write(0, IID_IDXGIFactory4, 0, 16);

            // Try different calling conventions
            System.out.println("  Trying invokeInt with [this, riid, ppv]...");
            Object[] qargs = { factory, iidMem, qiRef };
            int qhr = qiFn.invokeInt(qargs);
            System.out.printf("  QI returned hr=0x%s, ppv=%s%n", Integer.toHexString(qhr), qiRef.getValue());
            if (qiRef.getValue() != null) {
                Pointer newPtr = qiRef.getValue();
                System.out.printf("  New ptr: %s (native=0x%016x)%n", newPtr, Pointer.nativeValue(newPtr));
                // Check vtable of new pointer
                System.out.println("  New ptr vtable:");
                for (int i = 0; i < 15; i++) {
                    long e = newPtr.getLong(i * 8);
                    System.out.printf("    [%d] = 0x%016x%n", i, e);
                }
            }
        } catch (Throwable t) {
            System.out.printf("  QI THREW: %s%n", t.getMessage());
            if (t.getCause() != null) {
                System.out.printf("  Cause: %s%n", t.getCause().getMessage());
            }
        }

        // Try calling AddRef to see if it works at all
        System.out.println("\n--- Calling AddRef ---");
        try {
            Function addrefFn = Function.getFunction(new Pointer(addrefAddr));
            long refcount = addrefFn.invokeLong(new Object[]{factory});
            System.out.printf("  AddRef() = %d%n", refcount);
        } catch (Throwable t) {
            System.out.printf("  AddRef THREW: %s%n", t.getMessage());
        }

        // Try reading the vtable at the QI address itself
        System.out.println("\n--- VTable at QI function address ---");
        Pointer qiObj = new Pointer(qiAddr);
        System.out.printf("qiObj = %s (native=0x%016x)%n", qiObj, Pointer.nativeValue(qiObj));
        for (int i = 0; i < 5; i++) {
            long e = qiObj.getLong(i * 8);
            System.out.printf("  [%d] = 0x%016x%n", i, e);
        }
        long qiQi = qiObj.getLong(0);
        long qiAddref = qiObj.getLong(8);
        long qiRelease = qiObj.getLong(16);
        System.out.printf("  QI->QI=0x%016x AddRef=0x%016x Release=0x%016x%n", qiQi, qiAddref, qiRelease);

        // Try calling the QI function AT the QI address
        System.out.println("\n--- Calling QI-of-QI ---");
        try {
            Function qiOfQi = Function.getFunction(new Pointer(qiQi));
            PointerByReference qref = new PointerByReference();
            Memory iidMem = new Memory(16);
            iidMem.write(0, IID_IDXGIFactory4, 0, 16);
            int qhr = qiOfQi.invokeInt(new Object[]{qiObj, iidMem, qref});
            System.out.printf("  QI-of-QI: hr=0x%s, ppv=%s%n", Integer.toHexString(qhr), qref.getValue());
        } catch (Throwable t) {
            System.out.printf("  QI-of-QI THREW: %s%n", t.getMessage());
        }

        // Now try calling EnumAdapters directly at the factory using the ADDRESSES we CAN see
        System.out.println("\n--- EnumAdapters at various offsets ---");
        // From earlier debug, we saw valid pointers at offsets 0x28, 0x30, etc.
        // Let's try calling those as functions
        long validPtr1 = factory.getLong(0x28); // index 4 or 5?
        long validPtr2 = factory.getLong(0x30);
        System.out.printf("  Addr at [4]=0x%016x, [5]=0x%016x%n", validPtr1, validPtr2);

        for (int idx = 0; idx < 15; idx++) {
            long addr = factory.getLong(idx * 8);
            if (addr != 0 && addr != qiAddr && addr != addrefAddr) {
                System.out.printf("  Non-zero at [%d] = 0x%016x%n", idx, addr);
            }
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
