package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

/** Minimal debug test: compare JNA Pointer.getLong vs direct read. */
public class JnaPointerDebug {
    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    private static final byte[] IID_IDXGIFactory1 = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);

    public static void main(String[] args) throws Exception {
        System.out.println("=== JNA Pointer Debug ===\n");

        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory1), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));

        Pointer p1 = ref.getValue();
        long rawAddr = Pointer.nativeValue(p1);
        System.out.printf("p1=%s  rawAddr=0x%s%n", p1, Long.toHexString(rawAddr));

        // Compare: p1.getLong() vs direct ByteBuffer read via Memory copy
        // Method 1: p1.getLong(offset)
        System.out.println("\n  Via p1.getLong():");
        for (int i = 0; i < 20; i++) {
            System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(p1.getLong(i * 8)));
        }

        // Method 2: Use Memory as a scratch buffer, then read from it
        // But we need to COPY data from rawAddr into the Memory first
        // Use Memory.getByteArray to read from p1
        Memory scratch = new Memory(160);
        // p1 is a Pointer to the COM object; we can read directly from it
        // Let's try reading the first 160 bytes into a byte array
        byte[] data = p1.getByteArray(0, 160);
        System.out.println("\n  Via p1.getByteArray():");
        for (int i = 0; i < 20; i++) {
            long val = java.nio.ByteBuffer.wrap(data, i * 8, 8).getLong(0);
            System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(val));
        }

        // Method 3: Use scratch Memory and copy
        scratch.write(0, data, 0, data.length);
        System.out.println("\n  Via scratch Memory.getLong():");
        for (int i = 0; i < 20; i++) {
            System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(scratch.getLong(i * 8)));
        }

        // Now try calling EnumAdapters via QueryInterface path
        // First, QueryInterface to get IDXGIFactory4
        byte[] iidFactory4 = makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
            (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A);
        PointerByReference qref = new PointerByReference();
        int qhr = callQueryInterface(p1, writeGuidBytes(iidFactory4), qref);
        System.out.printf("\n  QueryInterface(IDXGIFactory4) hr=0x%s out=%s%n",
            Integer.toHexString(qhr), qref.getValue());

        if (qhr == 0 && qref.getValue() != null && !qref.getValue().equals(Pointer.NULL)) {
            Pointer factory4 = new Pointer(Pointer.nativeValue(qref.getValue()));
            System.out.println("  VTable at IDXGIFactory4 ptr:");
            for (int i = 0; i < 30; i++) {
                long val = factory4.getLong(i * 8);
                System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(val));
            }
            // EnumAdapters is at idx 7 (IDXGIFactory base)
            PointerByReference eeref = new PointerByReference();
            int ehr = callQueryInterface(factory4, writeGuidBytes(iidFactory4), eeref);
            System.out.printf("  QI on factory4 for Factory4: hr=0x%s out=%s%n",
                Integer.toHexString(ehr), eeref.getValue());
        }

        System.out.println("\nDone.");
    }

    static int callQueryInterface(Pointer object, Pointer riid, PointerByReference ppvObject) throws Exception {
        Pointer vtable = object;
        long fnAddr = vtable.getLong(0); // QI is at index 0
        if (fnAddr == 0) throw new RuntimeException("Null QI vtable entry");
        Function fn = Function.getFunction(new Pointer(fnAddr));
        Object result = fn.invoke(int.class, object, riid, ppvObject);
        return ((Number) result).intValue();
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
