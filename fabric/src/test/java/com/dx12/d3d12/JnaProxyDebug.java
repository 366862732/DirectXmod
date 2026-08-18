package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;

/**
 * Diagnose what type of object CreateDXGIFactory1 actually returns.
 * Key question: Is it a JNA COM proxy? If so, how to extract the real COM pointer?
 */
public class JnaProxyDebug {
    private static final byte[] IID_IDXGIFactory1 = makeGuid(0x770AAE78L, 0xF26F, 0x4DBA,
        (byte)0xA8,(byte)0x29,(byte)0x25,(byte)0x3C,(byte)0x83,(byte)0xD1,(byte)0xB3,(byte)0x87);

    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== JNA COM Proxy Debug ===\n");

        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory1), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));

        Pointer p = ref.getValue();
        System.out.printf("Pointer: %s%n", p);
        System.out.printf("nativeValue: 0x%016x%n", Pointer.nativeValue(p));
        System.out.printf("getClass: %s%n", p.getClass().getName());
        System.out.printf("class hierarchy:%n");
        Class<?> c = p.getClass();
        while (c != null) {
            System.out.printf("  -> %s%n", c.getName());
            // Print declared fields
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object val = f.get(p);
                    System.out.printf("     field %s = %s (type=%s)%n",
                        f.getName(), val, val != null ? val.getClass().getName() : "null");
                } catch (Exception e) {
                    System.out.printf("     field %s = ERROR: %s%n", f.getName(), e.getMessage());
                }
            }
            c = c.getSuperclass();
        }

        // Check if this is a JNA proxy by looking for specific fields
        System.out.println("\n--- Scanning for hidden/com fields ---");
        c = p.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object val = f.get(p);
                    if (val instanceof Pointer) {
                        Pointer ptrVal = (Pointer) val;
                        System.out.printf("  Field '%s' (type=%s) -> Pointer(0x%016x)%n",
                            f.getName(), f.getType().getSimpleName(), Pointer.nativeValue(ptrVal));
                        // Dump vtable of this pointer
                        for (int i = 0; i < 5; i++) {
                            long entry = ptrVal.getLong(i * 8);
                            System.out.printf("    [%d] = 0x%016x%n", i, entry);
                        }
                    } else if (val instanceof Memory) {
                        Memory mem = (Memory) val;
                        int len = (int) Math.min(64, mem.size());
                        byte[] data = mem.getByteArray(0, len);
                        StringBuilder hex = new StringBuilder();
                        for (byte b : data) hex.append(String.format("%02x ", b & 0xFF));
                        System.out.printf("  Field '%s' (type=Memory, size=%d): %s%n",
                            f.getName(), mem.size(), hex.toString().trim());
                    } else {
                        System.out.printf("  Field '%s' = %s (type=%s)%n",
                            f.getName(), val, val != null ? val.getClass().getName() : "null");
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            c = c.getSuperclass();
        }

        // Also dump the raw memory at the original pointer address
        System.out.println("\n--- Raw memory at original pointer (first 128 bytes) ---");
        byte[] raw = p.getByteArray(0, 128);
        for (int i = 0; i < 128; i += 16) {
            StringBuilder sb = new StringBuilder();
            StringBuilder ascii = new StringBuilder();
            for (int j = 0; j < 16; j++) {
                int b = raw[i + j] & 0xFF;
                sb.append(String.format("%02x ", b));
                ascii.append(b >= 32 && b < 127 ? (char) b : '.');
            }
            System.out.printf("  %04x: %s  %s%n", i, sb.toString().trim(), ascii);
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
