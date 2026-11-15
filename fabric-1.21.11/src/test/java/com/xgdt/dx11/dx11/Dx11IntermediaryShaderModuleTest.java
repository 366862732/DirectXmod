package com.xgdt.dx11.dx11;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class Dx11IntermediaryShaderModuleTest {
    private static final String TERRAIN_HLSL = """
        struct SPIRV_Cross_Input
        {
            float3 Position : POSITION;
            float4 Color : TEXCOORD0;
            float2 UV0 : TEXCOORD1;
            int2 UV2 : TEXCOORD2;
            float3 Normal : TEXCOORD3;
        };

        struct SPIRV_Cross_Output
        {
            float4 gl_Position : SV_Position;
        };

        SPIRV_Cross_Output main(SPIRV_Cross_Input stage_input)
        {
            float3 Position = stage_input.Position;
            float4 Color = stage_input.Color;
            float2 UV0 = stage_input.UV0;
            int2 UV2 = stage_input.UV2;
            float3 Normal = stage_input.Normal;
            SPIRV_Cross_Output stage_output;
            return stage_output;
        }
        """;

    @Test
    void dropsVertexInputsMissingFromTheCompactFormat() {
        String stripped = Dx11IntermediaryShaderModule.removeInputMembers(
            TERRAIN_HLSL, Set.of("Normal"), null, Set.of(), true);

        assertTrue(stripped.contains("Position : POSITION"));
        assertTrue(stripped.contains("Color : TEXCOORD0"));
        assertTrue(stripped.contains("UV0 : TEXCOORD1"));
        assertTrue(stripped.contains("UV2 : TEXCOORD2"));
        assertFalse(stripped.contains("Normal : TEXCOORD3"));
        assertFalse(stripped.contains("stage_input.Normal"));
        assertTrue(stripped.contains("SPIRV_Cross_Output main(SPIRV_Cross_Input stage_input)"));
    }

    @Test
    void keepsSystemValueInputsWhenStrippingVertexAttributes() {
        String hlsl = """
            struct SPIRV_Cross_Input
            {
                uint gl_VertexID : SV_VertexID;
                float3 Normal : TEXCOORD0;
            };

            SPIRV_Cross_Output main(SPIRV_Cross_Input stage_input)
            {
                uint id = stage_input.gl_VertexID;
                float3 normal = stage_input.Normal;
                SPIRV_Cross_Output stage_output;
                return stage_output;
            }
            """;

        String stripped = Dx11IntermediaryShaderModule.removeInputMembers(
            hlsl, Set.of("Normal", "gl_VertexID"), null, Set.of(), true);

        assertTrue(stripped.contains("gl_VertexID : SV_VertexID"));
        assertTrue(stripped.contains("stage_input.gl_VertexID"));
        assertFalse(stripped.contains("Normal : TEXCOORD0"));
        assertFalse(stripped.contains("stage_input.Normal"));
    }
}
