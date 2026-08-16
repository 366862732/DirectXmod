import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CheckFormats {
    public static void main(String[] args) {
        System.out.println("=== GpuFormat ordinals ===");
        for (GpuFormat f : GpuFormat.values()) {
            System.out.println("  ordinal=" + f.ordinal() + " " + f.name());
        }
        System.out.println();
        System.out.println("=== DefaultVertexFormat.POSITION_TEX_COLOR ===");
        VertexFormat fmt = DefaultVertexFormat.POSITION_TEX_COLOR;
        System.out.println("  vertexSize = " + fmt.getVertexSize());
        System.out.println("  elements:");
        for (VertexFormatElement e : fmt.getElements()) {
            System.out.println("    name=" + e.name() + " format=" + e.format() + " ordinal=" + e.format().ordinal() + " id=" + e.id() + " type=" + e.type());
        }
        System.out.println();
        System.out.println("=== DefaultVertexFormat.BLOCK ===");
        VertexFormat blk = DefaultVertexFormat.BLOCK;
        System.out.println("  vertexSize = " + blk.getVertexSize());
        System.out.println("  elements:");
        for (VertexFormatElement e : blk.getElements()) {
            System.out.println("    name=" + e.name() + " format=" + e.format() + " ordinal=" + e.format().ordinal() + " id=" + e.id() + " type=" + e.type());
        }
    }
}
