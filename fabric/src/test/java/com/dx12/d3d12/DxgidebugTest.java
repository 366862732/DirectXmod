package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

/** Minimal debug test for DXGI factory vtable access. */
public class DxgidebugTest {
    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    private static final byte[] IID_IDXGIFactory1 = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);

    public static void main(String[] args) throws Exception {
        System.out.println("=== DXGI Factory VTable Debug ===\n");

        // Step 1: Create factory
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory1), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s ref=%s%n", Integer.toHexString(hr), ref.getValue());

        Pointer p1 = ref.getValue();
        System.out.printf("  p1=%s  native=%s  isNull=%b%n",
            p1, Long.toHexString(Pointer.nativeValue(p1)), p1.equals(Pointer.NULL));

        // Step 2: Read vtable at p1
        System.out.println("\n  VTable at p1:");
        for (int i = 0; i < 20; i++) {
            long val = p1.getLong(i * 8);
            System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(val));
        }

        // Step 3: Create new Pointer from native value
        long nativeAddr = Pointer.nativeValue(p1);
        Pointer p2 = new Pointer(nativeAddr);
        System.out.printf("\n  p2=%s  native=%s%n", p2, Long.toHexString(Pointer.nativeValue(p2)));
        System.out.println("  VTable at p2:");
        for (int i = 0; i < 20; i++) {
            long val = p2.getLong(i * 8);
            System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(val));
        }

        // Step 4: Try QueryInterface to get IDXGIFactory2
        // IID_IDXGIFactory2 = {5B68F91D-A938-49F8-97F4-8A8D4E0E6C3A} -- need real value
        // Actually use a known valid one from the docs
        byte[] iidFactory2 = makeGuid(0x5492A102L, 0xD2B2, 0x453D,
            (byte)0x9F,(byte)0x76,(byte)0x3B,(byte)0x37,(byte)0x69,(byte)0x76,(byte)0x4F,(byte)0x88);
        PointerByReference qref = new PointerByReference();
        int qhr = invokeVTable(p2, 0, writeGuidBytes(iidFactory2), qref);
        System.out.printf("\n  QueryInterface(IDXGIFactory2) hr=0x%s  out=%s%n",
            Integer.toHexString(qhr), qref.getValue());

        if (qhr == 0 && qref.getValue() != null && !qref.getValue().equals(Pointer.NULL)) {
            Pointer factory2 = new Pointer(Pointer.nativeValue(qref.getValue()));
            System.out.println("  VTable at IDXGIFactory2 ptr:");
            for (int i = 0; i < 20; i++) {
                long val = factory2.getLong(i * 8);
                System.out.printf("    [%d] = 0x%s%n", i, Long.toHexString(val));
            }
            // Try EnumAdapters on factory2
            PointerByReference eeref = new PointerByReference();
            int ehr = invokeVTable(factory2, 7, 0, eeref);
            System.out.printf("  EnumAdapters(0) on factory2: hr=0x%s  out=%s%n",
                Integer.toHexString(ehr), eeref.getValue());
        }

        // Step 5: Also try calling EnumAdapters directly on p2 (original factory)
        System.out.println("\n  Trying EnumAdapters on p2 (original):");
        PointerByReference eeref2 = new PointerByReference();
        int ehr2 = invokeVTable(p2, 7, 0, eeref2);
        System.out.printf("  EnumAdapters(0): hr=0x%s  out=%s%n",
            Integer.toHexString(ehr2), eeref2.getValue());

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

    static int invokeVTable(Pointer object, int vtableIndex, Object... args) throws Exception {
        Pointer vtable = object;
        long fnAddr = vtable.getLong(vtableIndex * 8);
        if (fnAddr == 0) throw new RuntimeException("Null vtable entry at index " + vtableIndex);
        Function fn = Function.getFunction(new Pointer(fnAddr));
        Object result = fn.invoke(int.class, args);
        return ((Number) result).intValue();
    }
}
