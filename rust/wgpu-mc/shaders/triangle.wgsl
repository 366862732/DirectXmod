// Simple triangle vertex shader
// Attributes:
//   position: vec2<f32> — vertex position (x, y)
//   color: vec3<f32> — vertex color (r, g, b)
// Outputs:
//   @location(0) color: vec3<f32> — interpolated color per fragment

@vertex
fn vs_main(@location(0) position: vec2<f32>, @location(1) color: vec3<f32>) -> @builtin(position) vec4<f32> {
    return vec4<f32>(position, 0.0, 1.0);
}

@fragment
fn fs_main(@location(0) color: vec3<f32>) -> @location(0) vec4<f32> {
    return vec4<f32>(color, 1.0);
}
