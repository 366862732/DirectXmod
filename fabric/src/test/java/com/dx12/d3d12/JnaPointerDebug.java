package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.lang.reflect.*;

/** Debug: understand what type of object CreateDXGIFactory1 returns. */
public class JnaPointerDebug {
    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    private static final byte[] IID_IDXGIFactory4 = makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
        (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Deep Pointer Debug ===\n");

        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory4), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));

        Pointer p1 = ref.getValue();
        System.out.printf("p1 class: %s%n", p1.getClass().getName());
        System.out.printf("p1 toString: %s%n", p1.toString());
        System.out.printf("p1 hashCode: %d%n", p1.hashCode());

        // Check if it's a proxy
        System.out.println("\n  Is proxy?");
        System.out.printf("    instanceof Library: %b%n", p1 instanceof Library);
        System.out.printf("    instanceof Pointer: %b%n", p1 instanceof Pointer);

        // Check parent class
        Class<?> c = p1.getClass();
        while (c != null) {
            System.out.printf("    class: %s%n", c.getName());
            c = c.getSuperclass();
        }

        // Try reading vtable via reflection to get the raw address field
        System.out.println("\n  Raw address field:");
        for (Field f : Pointer.class.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object val = f.get(p1);
                System.out.printf("    %s = %s (type=%s)%n", f.getName(), val, val != null ? val.getClass().getName() : "null");
            } catch (Exception e) {
                System.out.printf("    %s = ERROR: %s%n", f.getName(), e.getMessage());
            }
        }

        // Read vtable
        long rawAddr = Pointer.nativeValue(p1);
        System.out.printf("\n  nativeValue: 0x%s%n", Long.toHexString(rawAddr));

        System.out.println("  VTable via getLong:");
        for (int i = 0; i < 30; i++) {
            long val = p1.getLong(i * 8);
            System.out.printf("    [%2d] = 0x%016x%n", i, val);
        }

        // Try creating a NEW Pointer from the raw address
        System.out.println("\n  Creating new Pointer from raw address:");
        Pointer p2 = new Pointer(rawAddr);
        System.out.printf("  p2 class: %s%n", p2.getClass().getName());
        System.out.printf("  p2 nativeValue: 0x%s%n", Long.toHexString(Pointer.nativeValue(p2)));
        System.out.println("  VTable via p2.getLong:");
        for (int i = 0; i < 30; i++) {
            long val = p2.getLong(i * 8);
            System.out.printf("    [%2d] = 0x%016x%n", i, val);
        }

        // Try calling QI through p2
        System.out.println("\n  Calling QI via p2:");
        try {
            long qiAddr = p2.getLong(0);
            System.out.printf("    QI addr: 0x%s%n", Long.toHexString(qiAddr));
            Function qiFn = Function.getFunction(new Pointer(qiAddr));
            PointerByReference qref = new PointerByReference();
            int qhr = qiFn.invokeInt(new Object[]{p2, writeGuidBytes(IID_IDXGIFactory4), qref});
            System.out.printf("    QI hr=0x%s out=%s%n", Integer.toHexString(qhr), qref.getValue());
        } catch (Throwable t) {
            System.out.printf("    QI threw: %s%n", t.getMessage());
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
