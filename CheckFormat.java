import java.lang.reflect.*;
import java.util.*;

public class CheckFormat {
    public static void main(String[] args) throws Exception {
        String jar = args[0];
        ClassLoader cl = new java.net.URLClassLoader(
            new java.net.URL[]{new java.io.File(jar).toURI().toURL()});

        Class<?> gpuFmtClass = cl.loadClass("com.mojang.blaze3d.GpuFormat");
        Object[] values = (Object[]) gpuFmtClass.getMethod("values").invoke(null);
        Map<String, Integer> fmtMap = new LinkedHashMap<>();
        for (Object v : values) {
            fmtMap.put(v.toString(), (Integer) gpuFmtClass.getMethod("ordinal").invoke(v));
        }
        System.out.println("=== GpuFormat ordinals ===");
        for (var e : fmtMap.entrySet()) {
            System.out.println("  ordinal=" + e.getValue() + " " + e.getKey());
        }

        Class<?> dvfClass = cl.loadClass("com.mojang.blaze3d.vertex.DefaultVertexFormat");
        System.out.println("\n=== DefaultVertexFormat color/position formats ===");

        Field posField = dvfClass.getDeclaredField("POSITION_FORMAT");
        posField.setAccessible(true);
        Object posFmt = posField.get(null);
        int posOrd = (int) gpuFmtClass.getMethod("ordinal").invoke(posFmt);
        System.out.println("POSITION_FORMAT = " + posFmt + " ordinal=" + posOrd);

        Field colorField = dvfClass.getDeclaredField("COLOR_FORMAT");
        colorField.setAccessible(true);
        Object colorFmt = colorField.get(null);
        int colorOrd = (int) gpuFmtClass.getMethod("ordinal").invoke(colorFmt);
        System.out.println("COLOR_FORMAT = " + colorFmt + " ordinal=" + colorOrd);

        Field uv0Field = dvfClass.getDeclaredField("UV0_FORMAT");
        uv0Field.setAccessible(true);
        Object uv0Fmt = uv0Field.get(null);
        int uv0Ord = (int) gpuFmtClass.getMethod("ordinal").invoke(uv0Fmt);
        System.out.println("UV0_FORMAT = " + uv0Fmt + " ordinal=" + uv0Ord);

        Class<?> fmtClass = cl.loadClass("com.mojang.blaze3d.vertex.VertexFormat");
        Object posTexColor = dvfClass.getField("POSITION_TEX_COLOR").get(null);
        int vs = (int) fmtClass.getMethod("getVertexSize").invoke(posTexColor);
        System.out.println("\nPOSITION_TEX_COLOR vertexSize = " + vs);

        Method blockSize = gpuFmtClass.getMethod("blockSize");
        System.out.println("POSITION blockSize = " + blockSize.invoke(posFmt));
        System.out.println("COLOR blockSize = " + blockSize.invoke(colorFmt));
        System.out.println("UV0 blockSize = " + blockSize.invoke(uv0Fmt));

        Class<?> vfeClass = cl.loadClass("com.mojang.blaze3d.vertex.VertexFormatElement");
        Object elements = fmtClass.getMethod("getElements").invoke(posTexColor);
        List<Object> elemList = new ArrayList<>();
        for (Object e : (Object[]) elements) {
            elemList.add(e);
        }
        System.out.println("\n=== POSITION_TEX_COLOR elements ===");
        for (int i = 0; i < elemList.size(); i++) {
            Object e = elemList.get(i);
            String name = (String) vfeClass.getMethod("name").invoke(e);
            Object fmt = vfeClass.getMethod("format").invoke(e);
            int off = (int) vfeClass.getMethod("offset").invoke(e);
            int ord = (int) gpuFmtClass.getMethod("ordinal").invoke(fmt);
            int bs = (int) blockSize.invoke(fmt);
            System.out.println("  [" + i + "] " + name + " format=" + fmt + " ordinal=" + ord + " offset=" + off + " blockSize=" + bs);
        }
    }
}
