package com.xgdt.dx12.shaderpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * {@code shaders/pipeline.json} 的解析结果。
 *
 * <p>结构借鉴 Iris/OptiFine 的 pass 链（composite → compositeN → final），
 * 但着色器是 HLSL，且用 JSON 显式声明输入 / 输出，不再依赖文件命名约定。
 *
 * <pre>
 * {
 *   "version": 1,
 *   "passes": [
 *     {
 *       "name": "composite",
 *       "vertex": "shaders/fullscreen.hlsl",   // 可选，缺省用内置全屏三角形 VS
 *       "fragment": "shaders/composite.hlsl",
 *       "inputs": ["scene_color", "scene_depth"],
 *       "output": "fx:0",                      // fx:N = 中间纹理；scene_color = 主目标
 *       "params": [1.0, 0.0, 0.0, 0.0],
 *       "enabled": true
 *     },
 *     {
 *       "name": "final",
 *       "fragment": "shaders/final.hlsl",
 *       "inputs": ["fx:0"],
 *       "output": "scene_color"
 *     },
 *     {
 *       "name": "simulate",                    // compute pass（cs_5_1）
 *       "type": "compute",
 *       "fragment": "shaders/simulate.hlsl",   // compute 里该字段承载 CS 源码
 *       "groups": [64, 1, 1],                  // 工作组数；缺省由 params/视口推导
 *       "buffers": [                           // SSBO 声明（结构化缓冲）
 *         { "name": "particles", "stride": 32, "count": 4096, "access": "uav" }
 *       ],
 *       "params": [1.0, 0.0, 0.0, 0.0]
 *     }
 *   ]
 * }
 * </pre>
 */
@Environment(EnvType.CLIENT)
public final class ShaderPackManifest {
    public static final int SUPPORTED_VERSION = 1;

    private final int version;
    private final List<Pass> passes;
    /** 包是否用程序化天空（含体积云）替换原版天空；为 true 时屏蔽原版方块云。 */
    private final boolean replacesVanillaSky;

    private ShaderPackManifest(int version, List<Pass> passes, boolean replacesVanillaSky) {
        this.version = version;
        this.passes = passes;
        this.replacesVanillaSky = replacesVanillaSky;
    }

    public int version() {
        return this.version;
    }

    public List<Pass> passes() {
        return this.passes;
    }

    public boolean replacesVanillaSky() {
        return this.replacesVanillaSky;
    }

    /** 最大 fx 索引 + 1（用于分配中间纹理）。 */
    public int intermediateCount() {
        int max = -1;
        for (Pass pass : this.passes) {
            max = Math.max(max, pass.outputTargetIndex());
            for (String input : pass.inputs()) {
                max = Math.max(max, indexOf(input));
            }
        }
        return max + 1;
    }

    private static int indexOf(String target) {
        if (target == null || !target.startsWith("fx:")) {
            return -1;
        }
        try {
            return Integer.parseInt(target.substring(3).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static ShaderPackManifest parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int version = root.has("version") ? root.get("version").getAsInt() : SUPPORTED_VERSION;
        if (version > SUPPORTED_VERSION) {
            throw new IllegalArgumentException("Unsupported pipeline.json version " + version
                + " (max " + SUPPORTED_VERSION + ")");
        }
        List<Pass> passes = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("passes");
        if (array == null) {
            throw new IllegalArgumentException("pipeline.json is missing the \"passes\" array");
        }
        for (JsonElement element : array) {
            passes.add(Pass.parse(element.getAsJsonObject()));
        }
        boolean replacesVanillaSky = root.has("replaceVanillaSky")
            && root.get("replaceVanillaSky").getAsBoolean();
        return new ShaderPackManifest(version, List.copyOf(passes), replacesVanillaSky);
    }

    /** 单个后处理 pass（或 compute pass）。 */
    @Environment(EnvType.CLIENT)
    public static final class Pass {
        private final String name;
        private final String type;
        private final String vertex;
        private final String fragment;
        private final List<String> inputs;
        private final List<BufferDecl> buffers;
        private final int[] groups;
        private final String output;
        private final float[] params;
        private final boolean enabled;

        private Pass(String name, String type, String vertex, String fragment, List<String> inputs,
                     List<BufferDecl> buffers, int[] groups, String output, float[] params,
                     boolean enabled) {
            this.name = name;
            this.type = type;
            this.vertex = vertex;
            this.fragment = fragment;
            this.inputs = inputs;
            this.buffers = buffers;
            this.groups = groups;
            this.output = output;
            this.params = params;
            this.enabled = enabled;
        }

        public String name() {
            return this.name;
        }

        /**
         * pass 类型：{@code composite}（默认，全屏三角形 + HLSL 片元）、
         * {@code raytrace}（native DXR：深度重建世界位置 → 太阳阴影可见性）
         * 或 {@code compute}（cs_5_1 计算着色器，只能读写结构化缓冲 SSBO）。
         */
        public String type() {
            return this.type;
        }

        public boolean isRaytrace() {
            return "raytrace".equals(this.type);
        }

        public boolean isCompute() {
            return "compute".equals(this.type);
        }

        /** SSBO 声明（按声明顺序：SRV 先占 t0..，UAV 先占 u0..）。 */
        public List<BufferDecl> buffers() {
            return this.buffers;
        }

        /** compute pass 的工作组数；全 0 表示由渲染器按视口 / params 推导。 */
        public int[] groups() {
            return this.groups.clone();
        }

        public int srvBufferCount() {
            int count = 0;
            for (BufferDecl decl : this.buffers) {
                if (!decl.unordered()) {
                    count++;
                }
            }
            return count;
        }

        public int uavBufferCount() {
            return this.buffers.size() - srvBufferCount();
        }

        /** 自定义顶点着色器路径；{@code null} = 使用内置全屏三角形 VS。 */
        public String vertex() {
            return this.vertex;
        }

        public String fragment() {
            return this.fragment;
        }

        public List<String> inputs() {
            return this.inputs;
        }

        public String output() {
            return this.output;
        }

        public float[] params() {
            return this.params.clone();
        }

        public boolean enabled() {
            return this.enabled;
        }

        public boolean writesSceneColor() {
            return "scene_color".equals(this.output);
        }

        public int outputTargetIndex() {
            return indexOf(this.output);
        }

        private static Pass parse(JsonObject json) {
            String name = json.has("name") ? json.get("name").getAsString() : "pass";
            String type = json.has("type") ? json.get("type").getAsString() : "composite";
            boolean raytrace = "raytrace".equals(type);
            boolean compute = "compute".equals(type);
            String vertex = json.has("vertex") && !json.get("vertex").isJsonNull()
                ? json.get("vertex").getAsString() : null;
            String fragment = json.has("fragment") ? json.get("fragment").getAsString() : null;
            if (!raytrace && (fragment == null || fragment.isBlank())) {
                throw new IllegalArgumentException("Pass '" + name + "' has no "
                    + (compute ? "compute shader" : "fragment shader"));
            }
            List<String> inputs = new ArrayList<>();
            if (json.has("inputs")) {
                for (JsonElement e : json.getAsJsonArray("inputs")) {
                    inputs.add(e.getAsString());
                }
            }
            List<BufferDecl> buffers = new ArrayList<>();
            if (json.has("buffers")) {
                for (JsonElement e : json.getAsJsonArray("buffers")) {
                    buffers.add(BufferDecl.parse(e.getAsJsonObject()));
                }
            }
            int[] groups = new int[3];
            if (json.has("groups")) {
                JsonArray g = json.getAsJsonArray("groups");
                for (int i = 0; i < 3 && i < g.size(); i++) {
                    groups[i] = g.get(i).getAsInt();
                }
            }
            String output = compute ? null
                : json.has("output") ? json.get("output").getAsString() : "scene_color";
            float[] params = new float[4];
            if (json.has("params")) {
                JsonArray p = json.getAsJsonArray("params");
                for (int i = 0; i < 4 && i < p.size(); i++) {
                    params[i] = p.get(i).getAsFloat();
                }
            }
            boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
            return new Pass(name, type, vertex, fragment, List.copyOf(inputs),
                List.copyOf(buffers), groups, output, params, enabled);
        }
    }

    /**
     * 结构化缓冲（SSBO）声明。
     *
     * <p>渲染器按 {@code stride × count} 分配一块 {@code Dx12GpuBuffer}；
     * {@code access = "srv"} 对应 HLSL 的 {@code StructuredBuffer<T>}，
     * {@code access = "uav"} 对应 {@code RWStructuredBuffer<T>}。
     * 每类按声明顺序从 {@code t0} / {@code u0} 起编号。
     */
    @Environment(EnvType.CLIENT)
    public record BufferDecl(String name, int stride, int count, boolean unordered) {
        static BufferDecl parse(JsonObject json) {
            String name = json.has("name") ? json.get("name").getAsString() : "buffer";
            int stride = json.has("stride") ? json.get("stride").getAsInt() : 16;
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            String access = json.has("access") ? json.get("access").getAsString() : "uav";
            if (stride <= 0 || count <= 0) {
                throw new IllegalArgumentException("Buffer '" + name
                    + "' has invalid stride/count: " + stride + "/" + count);
            }
            return new BufferDecl(name, stride, count, !"srv".equals(access));
        }

        /** 字节数（不低于 4，D3D12 结构化缓冲 stride 必须 4 字节对齐）。 */
        public long byteSize() {
            return (long) this.stride * this.count;
        }
    }
}
