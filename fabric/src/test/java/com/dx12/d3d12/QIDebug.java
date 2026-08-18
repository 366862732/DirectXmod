package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.util.Arrays;

public class QIDebug {
    private static final byte[] IID_IDXGIFactory1 = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);
    private static final byte[] IID_IDXGIFactory4 = makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
        (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A);

    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== QI Debug ===\n");

        // Create factory
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory4), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));
        Pointer factory = ref.getValue();
        long factoryAddr = Pointer.nativeValue(factory);
        System.out.printf("factory addr = 0x%016x%n", factoryAddr);

        // Read raw vtable
        System.out.println("\nFactory vtable:");
        for (int i = 0; i < 20; i++) {
            long entry = factory.getLong(i * 8);
            System.out.printf("  [%2d] = 0x%016x%n", i, entry);
        }

        // Read the underlying native memory directly using Memory
        System.out.println("\n--- Reading as raw bytes around factory address ---");
        // Try reading 256 bytes starting at factory address using a Memory object
        // We can't directly read arbitrary addresses with JNA Memory, but we CAN
        // use the factory Pointer to read
        byte[] raw = factory.getByteArray(0, 256);
        for (int i = 0; i < 256; i += 16) {
            StringBuilder sb = new StringBuilder();
            StringBuilder ascii = new StringBuilder();
            for (int j = 0; j < 16; j++) {
                int b = raw[i + j] & 0xFF;
                sb.append(String.format("%02x ", b));
                ascii.append(b >= 32 && b < 127 ? (char)b : '.');
            }
            System.out.printf("  +%03d: %s  %s%n", i, sb.toString().trim(), ascii);
        }

        // Key question: Is the factory a JNA proxy? Check if there's an internal pointer field
        System.out.println("\n--- Checking factory object internals ---");
        Class<?> c = factory.getClass();
        while (c != null) {
            System.out.printf("Class: %s%n", c.getName());
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object val = f.get(factory);
                    System.out.printf("  field '%s' = %s (type=%s)%n",
                        f.getName(), val, val != null ? val.getClass().getName() : "null");
                    // If it's a Pointer, dump its vtable
                    if (val instanceof Pointer) {
                        Pointer p = (Pointer) val;
                        System.out.printf("    -> Pointer(0x%016x):%n", Pointer.nativeValue(p));
                        for (int i = 0; i < 5; i++) {
                            long e = p.getLong(i * 8);
                            System.out.printf("       [%d] = 0x%016x%n", i, e);
                        }
                    }
                } catch (Exception e) {
                    System.out.printf("  field '%s' ERROR: %s%n", f.getName(), e.getMessage());
                }
            }
            c = c.getSuperclass();
        }

        // Now try calling QI with explicit this-pointer passing
        System.out.println("\n--- Attempting QI with explicit this pointer ---");
        long qiAddr = factory.getLong(0);
        long addrefAddr = factory.getLong(8);
        System.out.printf("QI=0x%016x AddRef=0x%016x%n", qiAddr, addrefAddr);

        // Try calling QI using Function.invokeInt with explicit this
        try {
            Pointer qiFnPtr = new Pointer(qiAddr);
            Function qiFn = Function.getFunction(qiFnPtr);
            PointerByReference qiRef = new PointerByReference();
            Memory iidMem = new Memory(16);
            iidMem.write(0, IID_IDXGIFactory1, 0, 16);

            // Build args array: [this, riid, ppv]
            Object[] qiArgs = new Object[]{factory, iidMem, qiRef};
            System.out.printf("  Calling QI with args: this=%s riid=%s ppvRef=%s%n",
                factory, iidMem, qiRef);
            System.out.printf("  this native=0x%016x, riid=0x%016x, ppvRef=0x%016x%n",
                Pointer.nativeValue(factory), Pointer.nativeValue(iidMem), Pointer.nativeValue(qiRef.getValue()));

            int qhr = qiFn.invokeInt(qiArgs);
            System.out.printf("  QI hr=0x%s ppv=%s (native=0x%s)%n",
                Integer.toHexString(qhr), qiRef.getValue(),
                qiRef.getValue() != null ? Long.toHexString(Pointer.nativeValue(qiRef.getValue())) : "null");
        } catch (Throwable t) {
            System.out.printf("  QI THREW: %s%n", t.getMessage());
            t.printStackTrace(System.out);
        }

        // Try calling AddRef to see if ANY call works
        System.out.println("\n--- Attempting AddRef ---");
        try {
            Pointer addrefFnPtr = new Pointer(addrefAddr);
            Function addrefFn = Function.getFunction(addrefFnPtr);
            long refcount = addrefFn.invokeLong(new Object[]{factory});
            System.out.printf("  AddRef() returned: %d%n", refcount);
        } catch (Throwable t) {
            System.out.printf("  AddRef THREW: %s%n", t.getMessage());
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
