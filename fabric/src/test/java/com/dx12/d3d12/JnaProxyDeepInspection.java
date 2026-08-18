package com.dx12.d3d12;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.lang.reflect.*;

/**
 * Deep introspection of JNA's internal COM proxy structure.
 */
public class JnaProxyDeepInspection {
    private static final byte[] IID_IDXGIFactory4 = makeGuid(0x1BC6EA02L, 0xEF36, 0x464F,
        (byte)0xBF,(byte)0x0C,(byte)0x21,(byte)0xCA,(byte)0x39,(byte)0xE5,(byte)0x16,(byte)0x8A);

    interface DxgiLib extends Library {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);
        int CreateDXGIFactory1(Pointer riid, PointerByReference ppFactory);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== JNA Proxy Deep Inspection ===\n");

        // Create factory
        PointerByReference ref = new PointerByReference();
        int hr = DxgiLib.INSTANCE.CreateDXGIFactory1(writeGuidBytes(IID_IDXGIFactory4), ref);
        System.out.printf("CreateDXGIFactory1 hr=0x%s%n", Integer.toHexString(hr));
        Pointer factory = ref.getValue();

        // Inspect Pointer class hierarchy and fields
        System.out.println("\n--- Pointer class hierarchy ---");
        Class<?> clazz = factory.getClass();
        while (clazz != null) {
            System.out.printf("Class: %s%n", clazz.getName());
            // Print all fields (including inherited)
            System.out.println("  Fields:");
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object val = f.get(factory);
                    System.out.printf("    %s.%s = %s (type=%s)%n",
                        clazz.getSimpleName(), f.getName(), val,
                        val != null ? val.getClass().getName() : "null");
                } catch (Exception e) {
                    System.out.printf("    %s.%s = ERROR: %s%n", clazz.getSimpleName(), f.getName(), e.getMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }

        // Check all interfaces
        System.out.println("\n--- Interfaces ---");
        for (Class<?> iface : factory.getClass().getInterfaces()) {
            System.out.printf("  %s%n", iface.getName());
        }

        // Check if it's a Library proxy
        System.out.println("\n--- Is Library proxy? ---");
        boolean isLibrary = false;
        for (Class<?> iface : factory.getClass().getInterfaces()) {
            if (Library.class.isAssignableFrom(iface)) {
                isLibrary = true;
                System.out.printf("  Implements Library: %s%n", iface.getName());
            }
        }
        if (!isLibrary) {
            // Check superclass interfaces
            Class<?> c = factory.getClass().getSuperclass();
            while (c != null) {
                for (Class<?> iface : c.getInterfaces()) {
                    if (Library.class.isAssignableFrom(iface)) {
                        isLibrary = true;
                        System.out.printf("  Superclass %s implements Library: %s%n", c.getName(), iface.getName());
                    }
                }
                c = c.getSuperclass();
            }
        }
        System.out.printf("  Is Library proxy: %b%n", isLibrary);

        // Read raw memory at factory address
        System.out.println("\n--- Raw memory at factory (first 128 bytes) ---");
        byte[] raw = factory.getByteArray(0, 128);
        for (int i = 0; i < 128; i += 16) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 16; j++) {
                sb.append(String.format("%02x ", raw[i + j] & 0xFF));
            }
            System.out.printf("  +%03d: %s%n", i, sb.toString().trim());
        }

        // Try to find the real COM pointer
        // Scan for addresses that point to dxgi.dll (0x7ff887...)
        System.out.println("\n--- Scanning for dxgi.dll function pointers ---");
        for (int i = 0; i < 128; i += 8) {
            long val = 0;
            for (int j = 0; j < 8; j++) {
                val |= ((long) raw[i + j] & 0xFF) << (j * 8);
            }
            if (val == 0) continue;
            if ((val & 0xFFFF000000000000L) == 0x7FF8000000000000L) {
                // Looks like a dxgi.dll address
                System.out.printf("  +%03d: 0x%016x (possible dxgi.dll function)%n", i, val);
            }
        }

        // Now try: cast factory to any Library interface
        System.out.println("\n--- Trying Library casts ---");
        // Try to create an IDXGIFactory4 proxy
        try {
            // Method 1: Direct cast
            Object proxy = factory;
            System.out.printf("  factory instanceof Pointer: %b%n", proxy instanceof Pointer);
            System.out.printf("  factory instanceof Library: %b%n", proxy instanceof Library);

            // Method 2: Use Library.newInstance
            // This is JNA's internal mechanism
            System.out.println("  Trying Library.newInstance...");
            try {
                Method newInstance = Library.class.getMethod("newInstance", Pointer.class, Class.class);
                Object libProxy = newInstance.invoke(null, factory, DxgiLib.class);
                System.out.printf("  newInstance succeeded: %s%n", libProxy);
            } catch (NoSuchMethodException e) {
                System.out.println("  newInstance not found (may not exist in this JNA version)");
            } catch (Exception e) {
                System.out.printf("  newInstance failed: %s%n", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        } catch (Exception e) {
            System.out.printf("  FAILED: %s%n", e.getMessage());
        }

        // Try to access the underlying native address through reflection
        System.out.println("\n--- Accessing Pointer internals ---");
        Field peerField = null;
        for (Field f : Pointer.class.getDeclaredFields()) {
            if (f.getName().equals("peer")) {
                peerField = f;
                break;
            }
        }
        if (peerField != null) {
            peerField.setAccessible(true);
            Object peer = peerField.get(factory);
            System.out.printf("  peer field: %s (type=%s)%n", peer, peer != null ? peer.getClass().getName() : "null");
            if (peer instanceof Long) {
                System.out.printf("  peer value: 0x%s%n", Long.toHexString((Long) peer));
            }
        } else {
            System.out.println("  'peer' field not found in Pointer class");
            // List all fields
            System.out.println("  Available fields:");
            for (Field f : Pointer.class.getDeclaredFields()) {
                System.out.printf("    %s (%s)%n", f.getName(), f.getType().getName());
            }
        }

        // Check Pointer.class for the address
        System.out.println("\n--- Address verification ---");
        System.out.printf("  Pointer.nativeValue(factory) = 0x%s%n",
            Long.toHexString(Pointer.nativeValue(factory)));

        // Try creating a new Pointer from the native value
        long nativeAddr = Pointer.nativeValue(factory);
        Pointer rawPtr = new Pointer(nativeAddr);
        System.out.printf("  new Pointer(nativeValue): %s%n", rawPtr);
        System.out.printf("  rawPtr.nativeValue = 0x%s%n", Long.toHexString(Pointer.nativeValue(rawPtr)));

        // Read vtable from raw pointer
        System.out.println("\n  VTable from raw pointer:");
        for (int i = 0; i < 5; i++) {
            System.out.printf("    [%d] = 0x%016x%n", i, rawPtr.getLong(i * 8));
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
