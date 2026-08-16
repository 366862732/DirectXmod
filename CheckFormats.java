import com.mojang.blaze3d.GpuFormat;
import java.lang.reflect.Field;

public class CheckFormats {
    public static void main(String[] args) throws Exception {
        // Load DefaultVertexFormat without triggering <clinit> by using Unsafe-like approach
        // Instead, let's just parse the bytecode constant pool

        byte[] classBytes = java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get("fabric/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-043a8b3edf/26.2/com/mojang/blaze3d/vertex/DefaultVertexFormat.class"));

        // Simple constant pool parser
        int cpCount = ((classBytes[8] & 0xFF) << 8) | (classBytes[9] & 0xFF);
        int idx = 10;
        Object[] cp = new Object[cpCount];
        for (int i = 1; i < cpCount; i++) {
            int tag = classBytes[idx] & 0xFF;
            if (tag == 9) { // FIELDREF
                int clazz = ((classBytes[idx+1] & 0xFF) << 8) | (classBytes[idx+2] & 0xFF);
                int nameType = ((classBytes[idx+3] & 0xFF) << 8) | (classBytes[idx+4] & 0xFF);
                cp[i] = new int[]{clazz, nameType};
                idx += 5;
            } else if (tag == 7) { // CLASS
                int nameIdx = ((classBytes[idx+1] & 0xFF) << 8) | (classBytes[idx+2] & 0xFF);
                cp[i] = nameIdx;
                idx += 3;
            } else if (tag == 8) { // STRING
                int strIdx = ((classBytes[idx+1] & 0xFF) << 8) | (classBytes[idx+2] & 0xFF);
                cp[i] = strIdx;
                idx += 3;
            } else if (tag == 10 || tag == 11) { // METHODREF/INTERFACEMETHODREF
                idx += 5;
            } else if (tag == 3 || tag == 4) { // INT/LONG
                idx += (tag == 4 ? 9 : 5);
            } else if (tag == 5 || tag == 6) { // FLOAT/DOUBLE
                idx += (tag == 6 ? 9 : 5);
            } else if (tag == 12) { // NAME_AND_TYPE
                idx += 4;
            } else if (tag == 1) { // UTF8
                int len = ((classBytes[idx+1] & 0xFF) << 8) | (classBytes[idx+2] & 0xFF);
                String s = new String(classBytes, idx+3, len, "UTF-8");
                cp[i] = s;
                idx += 3 + len;
            } else if (tag == 15 || tag == 16) { // METHOD_HANDLE/METHOD_TYPE
                idx += 3;
            } else {
                System.out.println("Unknown cp tag: " + tag + " at idx=" + idx);
                idx++;
            }
        }

        // Find COLOR_FORMAT and POSITION_FORMAT field references
        // From earlier analysis:
        // #68 = NameAndType for POSITION_FORMAT
        // #69 = Fieldref for DefaultVertexFormat.POSITION_FORMAT
        // #73 = NameAndType for COLOR_FORMAT
        // #74 = Fieldref for DefaultVertexFormat.COLOR_FORMAT

        // The fieldref points to GpuFormat static field. Let's find what value is stored.
        // Actually, let me just look at the constant pool entries directly

        // Print relevant constants
        for (int i = 1; i < cp.length; i++) {
            Object v = cp[i];
            if (v instanceof String s && (s.contains("FORMAT") || s.contains("POSITION") || s.contains("COLOR") || s.contains("UV0"))) {
                System.out.println("#" + i + " UTF8=" + s);
            }
        }
    }
}
