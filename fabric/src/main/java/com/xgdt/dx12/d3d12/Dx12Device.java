package com.xgdt.dx12.d3d12;

import com.xgdt.dx12.dx12.Dx12Native;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java D3D12 device factory.
 *
 * Adapter enumeration uses the existing JNI bridge (dx12_mc.dll) which
 * correctly calls CreateDXGIFactory1 + EnumAdapters on Windows.
 * Device creation can use either the JNI bridge or direct D3D12CreateDevice
 * via JNA (future work).
 */
public class Dx12Device implements AutoCloseable {

    // Feature levels
    public static final int D3D_FEATURE_LEVEL_12_0 = 0xC000;
    public static final int D3D_FEATURE_LEVEL_11_1 = 0xB000;
    public static final int D3D_FEATURE_LEVEL_11_0 = 0xA000;

    // =========================================================================
    // Public API
    // =========================================================================

    private final String adapterName;
    private final long adapterLuid;
    private final int vendorId;
    private final int deviceId;
    private final long dedicatedVideoMemory;

    private Dx12Device(String name, long luid, int vid, int did, long vram) {
        this.adapterName = name;
        this.adapterLuid = luid;
        this.vendorId = vid;
        this.deviceId = did;
        this.dedicatedVideoMemory = vram;
    }

    /**
     * Create a D3D12 device using the JNI bridge.
     * This calls dx12CreateDevice() which creates the global device context.
     * @return the created device, or null if no D3D12 adapter is available
     */
    public static Dx12Device create() {
        String result = Dx12Native.dx12CreateDevice();
        if (result.startsWith("ERROR:")) {
            throw new Dx12Exception(result, 0x80004005);
        }
        System.out.println("  Device created: " + result);
        // Parse the result to get adapter info
        String name = parseAdapterName(result);
        return new Dx12Device(name, 0L, 0, 0, 0L);
    }

    /**
     * Enumerate all D3D12-capable adapters using the JNI bridge.
     */
    public static List<Dx12AdapterInfo> enumerateAdapters() {
        List<Dx12AdapterInfo> result = new ArrayList<>();
        try {
            String json = Dx12Native.dx12EnumerateAdapters();
            System.out.printf("  enumerateAdapters JSON: %s%n", json);
            result = parseAdaptersJson(json);
        } catch (Exception e) {
            System.err.printf("Failed to enumerate adapters via JNI: %s%n", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // =========================================================================
    // Public getters
    // =========================================================================

    public String getAdapterName() { return adapterName; }
    public long getAdapterLuid() { return adapterLuid; }
    public int getVendorId() { return vendorId; }
    public int getDeviceId() { return deviceId; }
    public long getDedicatedVideoMemory() { return dedicatedVideoMemory; }

    @Override
    public void close() {
        System.out.println("D3D12 device released (Java side)");
    }

    // =========================================================================
    // JSON parsing (simple, no external deps)
    // =========================================================================

    private static String parseAdapterName(String result) {
        int semi = result.indexOf(';');
        if (semi > 0) return result.substring(0, semi).trim();
        return result.trim();
    }

    private static List<Dx12AdapterInfo> parseAdaptersJson(String json) {
        List<Dx12AdapterInfo> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        // Simple JSON parser for our specific format
        // Format: [{"name":"...","luid":"...","vid":0x...,"did":0x...,"vram_gb":N},...]
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end <= start) return result;
        String inner = json.substring(start + 1, end);

        int pos = 0;
        while (pos < inner.length()) {
            // Skip whitespace and commas
            while (pos < inner.length() && (inner.charAt(pos) == ' ' || inner.charAt(pos) == ',')) pos++;
            if (pos >= inner.length()) break;
            if (inner.charAt(pos) != '{') break;

            int objStart = pos;
            int braceDepth = 0;
            do {
                char c = inner.charAt(pos);
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
                pos++;
            } while (braceDepth > 0 && pos < inner.length());

            String obj = inner.substring(objStart, pos);
            Dx12AdapterInfo info = parseAdapterObj(obj);
            if (info != null) result.add(info);
        }
        return result;
    }

    private static Dx12AdapterInfo parseAdapterObj(String obj) {
        try {
            String name = extractString(obj, "name");
            String luidStr = extractString(obj, "luid");
            long luid = luidStr != null ? Long.parseUnsignedLong(luidStr, 16) : 0L;
            String vidStr = extractHex(obj, "vid");
            int vid = vidStr != null ? Integer.parseUnsignedInt(vidStr, 16) : 0;
            String didStr = extractHex(obj, "did");
            int did = didStr != null ? Integer.parseUnsignedInt(didStr, 16) : 0;
            long vramGb = extractNumber(obj, "vram_gb");
            long vramBytes = vramGb * 1024L * 1024 * 1024;
            System.out.printf("  Parsed: %s LUID=0x%s VID=0x%s DID=0x%s VRAM=%d GiB%n",
                name, luidStr, Integer.toHexString(vid), Integer.toHexString(did), vramGb);
            return Dx12AdapterInfo.of(name, luid, vid, did, vramBytes, 0, 0, 16384);
        } catch (Exception e) {
            System.err.printf("  Failed to parse adapter: %s err=%s%n", obj, e.getMessage());
            return null;
        }
    }

    private static long extractNumber(String json, String key) {
        int kpos = json.indexOf("\"" + key + "\"");
        if (kpos < 0) return 0L;
        int colon = json.indexOf(':', kpos + key.length() + 2);
        if (colon < 0) return 0L;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0L; }
    }

    private static String extractString(String json, String key) {
        int kpos = json.indexOf("\"" + key + "\"");
        if (kpos < 0) return null;
        int colon = json.indexOf(':', kpos + key.length() + 2);
        if (colon < 0) return null;
        int quote1 = json.indexOf('"', colon + 1);
        if (quote1 < 0) return null;
        int quote2 = json.indexOf('"', quote1 + 1);
        if (quote2 < 0) return null;
        return json.substring(quote1 + 1, quote2);
    }

    private static String extractHex(String json, String key) {
        int kpos = json.indexOf("\"" + key + "\"");
        if (kpos < 0) return null;
        int colon = json.indexOf(':', kpos + key.length() + 2);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || Character.toLowerCase(json.charAt(end)) >= 'a')) end++;
        String hex = json.substring(start, end).replace("0x", "");
        return hex.isEmpty() ? null : hex;
    }
}
