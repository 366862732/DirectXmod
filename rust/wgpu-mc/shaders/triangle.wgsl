@vertex
fn vs_main(@location(0) position: vec2<f32>, @location(1) color: vec3<f32>) -> MyOutput {
    var out: MyOutput;
    out.position = vec4<f32>(position, 0.0, 1.0);
    out.color = color;
    return out;
}

struct MyOutput {
    @builtin(position) position: vec4<f32>,
    @location(1) color: vec3<f32>,
}

@fragment
fn fs_main(input: MyOutput) -> @location(0) vec4<f32> {
    return vec4<f32>(input.color, 1.0);
}
