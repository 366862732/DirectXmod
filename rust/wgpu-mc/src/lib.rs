//! wgpu-mc: wgpu renderer with depth buffer, geometry pipeline, and readback.
//!
//! Architecture: Triple-buffered ring on JNI thread with optional surface mode.
//! - Surface mode:  render directly to window swapchain, present → no readback
//! - Offscreen mode: render to textures, async readback via triple-buffer ring
//!
//! Note: Push constants removed — not supported on all GPUs.
//! Model transforms are baked into vertex buffers at creation time.
//!
//! TDR Prevention (HWND sharing with OpenGL):
//! The D3D12 swapchain is created on the same HWND as MC's GL context.
//! To prevent GPU driver TDR, the GL context is temporarily detached
//! (GLFW.glfwMakeContextCurrent(0)) from the Java side before calling
//! renderFrame(), and reattached after. This ensures only one API has
//! access to the HWND at any time.

use bytemuck::{Pod, Zeroable};
use std::sync::mpsc;

#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct Vertex {
    position: [f32; 3],
    color: [f32; 3],
}

impl Vertex {
    const ATTRIBS: [wgpu::VertexAttribute; 2] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x3,
    ];
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

/// Chunk vertex with UV for texture atlas and lightmap sampling.
/// 40 bytes: position(12) + color(12) + uv(8) + light_uv(8).
/// light_uv is the lightmap texture coordinate for dynamic block/sky lighting.
#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct ChunkVertex {
    position: [f32; 3],
    color: [f32; 3],
    uv: [f32; 2],
    light_uv: [f32; 2],
}

impl ChunkVertex {
    const ATTRIBS: [wgpu::VertexAttribute; 4] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x3,
        2 => Float32x2,
        3 => Float32x2,
    ];
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

/// Particle vertex for point sprite rendering.
/// Particle billboard vertex: position + color + size(px) + corner offset.
/// 40 bytes. Rendered as a screen-space quad (6 verts per particle) because
/// D3D12 ignores `@builtin(point_size)` — point sprites are 1px there.
#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct ParticleVertex {
    position: [f32; 3],
    color: [f32; 4],
    size: f32,
    /// Corner offset in [-0.5, 0.5]² relative to the particle center.
    corner: [f32; 2],
}

impl ParticleVertex {
    const ATTRIBS: [wgpu::VertexAttribute; 4] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x4,
        2 => Float32,
        3 => Float32x2,
    ];
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

/// Sky dome vertex: position + normal (24 bytes).
/// normal is also the direction from origin for height-based color interpolation.
#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct SkyVertex {
    position: [f32; 3],
    normal: [f32; 3],
}

impl SkyVertex {
    const ATTRIBS: [wgpu::VertexAttribute; 2] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x3,
    ];
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

#[repr(C)]
#[derive(Copy, Clone, Debug, bytemuck::Pod, bytemuck::Zeroable)]
struct TexVertex {
    position: [f32; 2],
    uv: [f32; 2],
}

impl TexVertex {
    const ATTRIBS: [wgpu::VertexAttribute; 2] = wgpu::vertex_attr_array![
        0 => Float32x2,
        1 => Float32x2,
    ];

    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

// No push constants — compatible with all GPUs
const SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec3<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec3<f32>, @location(1) color: vec3<f32>) -> VertexOutput {
    var out: VertexOutput;
    // Offset local-space geometry by camera world position so it
    // stays visible near the camera regardless of player location.
    let world_pos = pos + camera.camera_pos;
    out.position = camera.view_proj * vec4<f32>(world_pos, 1.0);
    out.color = color;
    return out;
}

@fragment
fn fs_main(@location(0) color: vec3<f32>) -> @location(0) vec4<f32> {
    return vec4<f32>(color, 1.0);
}
"#;

// Chunk shader: samples atlas texture and lightmap for dynamic block/sky lighting.
// Lightmap bindings: @binding(3) = lightmap texture, @binding(4) = lightmap sampler.
const CHUNK_SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
    fog: vec4<f32>,  // fog.rgb = color, fog.a = density
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;
@group(0) @binding(1) var atlas: texture_2d<f32>;
@group(0) @binding(2) var atlas_sampler: sampler;
@group(0) @binding(3) var lightmap: texture_2d<f32>;
@group(0) @binding(4) var lightmap_sampler: sampler;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) tint: vec3<f32>,
    @location(2) linear_depth: f32,
    @location(3) light_uv: vec2<f32>,
}

@vertex
fn vs_main(
    @location(0) pos: vec3<f32>,
    @location(1) color: vec3<f32>,
    @location(2) uv: vec2<f32>,
    @location(3) light_uv: vec2<f32>,
) -> VertexOutput {
    var out: VertexOutput;
    // Positions are in world space (section origin + local offset).
    // The MVP (view_proj) already includes the view matrix that handles
    // world→camera transformation, so no camera_pos addition is needed.
    let world_pos = pos;
    out.position = camera.view_proj * vec4<f32>(world_pos, 1.0);
    out.uv = uv;
    out.tint = color;
    out.light_uv = light_uv;
    // clip_w = view-space distance (positive for in-front geometry).
    // Store the reciprocal so the fragment shader can compute distance = 1/linear_depth.
    out.linear_depth = out.position.w;
    return out;
}

/// Apply exponential fog to a fragment color.
/// clip_w is the clip-space w from the vertex shader.
/// For standard OpenGL projection (P[3][2] = -1), clip_w = -view_z (positive for visible geometry).
/// For JOML perspective projection, we use abs() to always get a positive distance.
fn apply_fog(color: vec4<f32>, clip_w: f32) -> vec4<f32> {
    let distance = abs(clip_w);
    let fog_factor = exp(-camera.fog.a * distance);
    let final_rgb = mix(camera.fog.rgb, color.rgb, fog_factor);
    return vec4<f32>(final_rgb, color.a);
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
    let tex_color = textureSample(atlas, atlas_sampler, in.uv);
    // Discard fully transparent pixels (e.g. plant background, leaf cutout).
    // This prevents fallback solid color bleeding through transparent areas
    // of the texture, which was causing plants to render as colored paper planes.
    let threshold = 0.05;
    if tex_color.a < threshold {
        discard;
    }
    // Sample lightmap for dynamic block/sky lighting.
    let light_color = textureSample(lightmap, lightmap_sampler, in.light_uv);
    let lit = vec4<f32>(tex_color.rgb * in.tint * light_color.rgb, tex_color.a);
    return apply_fog(lit, in.linear_depth);
}

@fragment
fn fs_transparent(in: VertexOutput) -> @location(0) vec4<f32> {
    let tex_color = textureSample(atlas, atlas_sampler, in.uv);
    // Discard both fully transparent pixels (gaps in cutout textures)
    // AND fully opaque pixels (already rendered by opaque pass).
    // Only semi-transparent pixels (e.g. water, stained glass, leaf edges)
    // pass through for alpha blending.
    let threshold_low = 0.05;
    let threshold_high = 0.95;
    if tex_color.a < threshold_low || tex_color.a > threshold_high {
        discard;
    }
    // Sample lightmap for dynamic block/sky lighting of transparent fragments.
    let light_color = textureSample(lightmap, lightmap_sampler, in.light_uv);
    let lit = vec4<f32>(tex_color.rgb * in.tint * light_color.rgb, tex_color.a);
    return apply_fog(lit, in.linear_depth);
}
"#;

// Textured fullscreen quad shader — renders GL framebuffer capture as D3D12 texture.
const TEX_SHADER_SRC: &str = r#"
struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec2<f32>, @location(1) uv: vec2<f32>) -> VertexOutput {
    var out: VertexOutput;
    out.position = vec4<f32>(pos, 0.0, 1.0);
    out.uv = uv;
    return out;
}

@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var samp: sampler;

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
    // Flip Y: GL framebuffer reads bottom-up, wgpu textures are top-down.
    var uv = in.uv;
    uv.y = 1.0 - uv.y;
    return textureSample(tex, samp, uv);
}
"#;

const IDENTITY: [[f32; 4]; 4] = [
    [1.0, 0.0, 0.0, 0.0],
    [0.0, 1.0, 0.0, 0.0],
    [0.0, 0.0, 1.0, 0.0],
    [0.0, 0.0, 0.0, 1.0],
];

/// Entity shader: renders colored boxes at entity positions.
/// Uses same camera uniform as main pipeline.
const ENTITY_SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
    fog: vec4<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec3<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec3<f32>, @location(1) color: vec3<f32>) -> VertexOutput {
    var out: VertexOutput;
    // Entity positions are in world space (absolute coordinates).
    out.position = camera.view_proj * vec4<f32>(pos, 1.0);
    out.color = color;
    return out;
}

@fragment
fn fs_main(@location(0) color: vec3<f32>) -> @location(0) vec4<f32> {
    return vec4<f32>(color, 1.0);
}
"#;

/// Particle shader: renders screen-space billboards (quads) with alpha
/// blending. D3D12 cannot size point sprites (`@builtin(point_size)` is a
/// no-op there), so each particle is expanded to a 2-triangle quad in NDC
/// space — the quad always faces the camera by construction.
const PARTICLE_SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
    fog: vec4<f32>,
    viewport: vec2<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec4<f32>,
    @location(1) uv: vec2<f32>,
}

@vertex
fn vs_main(
    @location(0) pos: vec3<f32>,
    @location(1) color: vec4<f32>,
    @location(2) size: f32,
    @location(3) corner: vec2<f32>,
) -> VertexOutput {
    var out: VertexOutput;
    let clip = camera.view_proj * vec4<f32>(pos, 1.0);
    // size is in screen pixels; convert to NDC units (NDC spans 2.0 across
    // the whole viewport) and scale by clip.w so the offset survives the
    // perspective divide. Depth stays at the particle's true depth.
    let ndc = vec2<f32>(size / camera.viewport.x, size / camera.viewport.y) * 2.0;
    out.position = vec4<f32>(
        clip.x + corner.x * ndc.x * clip.w,
        clip.y + corner.y * ndc.y * clip.w,
        clip.z,
        clip.w,
    );
    out.color = color;
    out.uv = corner + 0.5; // [0,1]² across the quad
    return out;
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
    // Soft circle sprite: discard outside the radius, feather the edge.
    let d = distance(in.uv, vec2<f32>(0.5));
    if d > 0.5 {
        discard;
    }
    let alpha = 1.0 - smoothstep(0.35, 0.5, d);
    return vec4<f32>(in.color.rgb, in.color.a * alpha);
}
"#;

// Sky dome shader: renders a gradient hemisphere at the horizon.
// Vertex color interpolated between deep blue (zenith) and fog color (horizon).
// Uses position + normal per vertex (24 bytes), pos is on the hemisphere surface.
const SKY_SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
    fog: vec4<f32>,
    sky_color_top: vec3<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec3<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec3<f32>, @location(1) normal: vec3<f32>) -> VertexOutput {
    var out: VertexOutput;
    let world_pos = pos + camera.camera_pos;
    out.position = camera.view_proj * vec4<f32>(world_pos, 1.0);
    // Sky gradient: zenith color at the top (t=1), fog color at the horizon (t=0).
    // The zenith color comes from the uniform (MC sky color via updateSky),
    // so the sky follows Minecraft's time-of-day / weather instead of a
    // hardcoded palette.
    let t = max(0.0, normal.y);
    out.color = mix(camera.fog.rgb, camera.sky_color_top, t);
    return out;
}

@fragment
fn fs_main(@location(0) color: vec3<f32>) -> @location(0) vec4<f32> {
    return vec4<f32>(color, 1.0);
}
"#;

const RING_SIZE: usize = 3;
const LERP_FACTOR: f32 = 0.85;

fn mat4_lerp(a: &[[f32; 4]; 4], b: &[[f32; 4]; 4], t: f32) -> [[f32; 4]; 4] {
    let mut result = [[0.0f32; 4]; 4];
    for r in 0..4 {
        for c in 0..4 {
            result[r][c] = a[r][c] + (b[r][c] - a[r][c]) * t;
        }
    }
    result
}

/// Write camera MVP + camera_pos + fog + sky_color + viewport to the uniform
/// buffer.
/// Layout:
///   mat4x4  view_proj      (64 bytes, offset 0)
///   vec3    camera_pos     (12 bytes, offset 64, padded to 16)
///   vec4    fog            (16 bytes, offset 80)  — fog.rgb = color, fog.a = density
///   vec3    sky_color_top  (12 bytes, offset 96, padded to 16)
///   vec2    viewport       (8 bytes,  offset 112, padded to 16)
/// Total: 128 bytes.
fn write_camera_uniform(
    queue: &wgpu::Queue,
    buffer: &wgpu::Buffer,
    mvp: &[[f32; 4]; 4],
    pos: &[f32; 3],
    fog: &[f32; 4],
    sky_top: &[f32; 3],
    viewport: &[f32; 2],
) {
    let mut data = [0u8; 128];
    data[0..64].copy_from_slice(bytemuck::cast_slice(mvp));
    data[64..76].copy_from_slice(bytemuck::cast_slice(pos));
    data[80..96].copy_from_slice(bytemuck::cast_slice(&fog[..]));
    data[96..108].copy_from_slice(bytemuck::cast_slice(&sky_top[..]));
    data[112..120].copy_from_slice(bytemuck::cast_slice(viewport));
    queue.write_buffer(buffer, 0, &data);
}

// ── Frustum culling helpers (Phase 11f) ──────────────────────────────

/// Transpose a 4×4 matrix.
///
/// The JNI layer copies the JOML `Matrix4f.get()` output (column-major
/// float[16]) straight into the row-major `[[f32; 4]; 4]` slots, so the
/// in-memory matrix is the *transpose* of the intended world→clip matrix.
/// The uniform upload path intentionally keeps that layout (WGSL
/// `mat4x4<f32>` uniforms are column-major too), but plane extraction below
/// expects row-major, so the camera matrix must be transposed back first.
fn transpose4(m: &[[f32; 4]; 4]) -> [[f32; 4]; 4] {
    [
        [m[0][0], m[1][0], m[2][0], m[3][0]],
        [m[0][1], m[1][1], m[2][1], m[3][1]],
        [m[0][2], m[1][2], m[2][2], m[3][2]],
        [m[0][3], m[1][3], m[2][3], m[3][3]],
    ]
}

/// Extract 6 frustum planes from a world→clip matrix (row-major storage,
/// D3D-style z ∈ [0, w] as produced by JOML perspective(..., zZeroToOne=true)).
/// Plane: a*x + b*y + c*z + d = 0 (normalized); a point is inside when dot > 0.
/// Order: left, right, bottom, top, near, far.
fn extract_frustum_planes(m: &[[f32; 4]; 4]) -> [[f32; 4]; 6] {
    let r1 = m[0];
    let r2 = m[1];
    let r3 = m[2];
    let r4 = m[3];
    let mut planes = [[0.0f32; 4]; 6];
    planes[0] = plane_combine(r4, r1, 1.0);  // left:   clip.x >= -clip.w
    planes[1] = plane_combine(r4, r1, -1.0); // right:  clip.x <=  clip.w
    planes[2] = plane_combine(r4, r2, 1.0);  // bottom: clip.y >= -clip.w
    planes[3] = plane_combine(r4, r2, -1.0); // top:    clip.y <=  clip.w
    planes[4] = plane_normalize(r3);         // near:   clip.z >= 0 (D3D z)
    planes[5] = plane_combine(r4, r3, -1.0); // far:    clip.z <=  clip.w
    planes
}

/// p = normalize(a + sign * b)
fn plane_combine(a: [f32; 4], b: [f32; 4], sign: f32) -> [f32; 4] {
    let mut p = [0.0f32; 4];
    for i in 0..4 {
        p[i] = a[i] + sign * b[i];
    }
    plane_normalize(p)
}

fn plane_normalize(p: [f32; 4]) -> [f32; 4] {
    let len = (p[0] * p[0] + p[1] * p[1] + p[2] * p[2]).sqrt();
    if len > 1e-8 {
        [p[0] / len, p[1] / len, p[2] / len, p[3] / len]
    } else {
        p
    }
}

/// Conservative AABB (world-space min/max) vs frustum test.
/// Returns true when the box is fully or partially inside the frustum.
fn aabb_in_frustum(min: [f32; 3], max: [f32; 3], planes: &[[f32; 4]; 6]) -> bool {
    for p in planes {
        // p-vertex: the corner farthest along the plane normal
        let px = if p[0] >= 0.0 { max[0] } else { min[0] };
        let py = if p[1] >= 0.0 { max[1] } else { min[1] };
        let pz = if p[2] >= 0.0 { max[2] } else { min[2] };
        let d = p[0] * px + p[1] * py + p[2] * pz + p[3];
        if d < 0.0 {
            return false; // entire box outside this plane
        }
    }
    true
}

/// Build the indirect draw list for all stored chunk meshes (Phase 11e),
/// skipping sections outside the view frustum (Phase 11f). Every visible
/// mesh yields one [`DrawIndexedIndirectArgs`] referencing the merged
/// vertex/index buffers.
/// Returns (draws, visible_sections, culled_sections).
fn collect_visible_draws<'m>(
    sections: impl Iterator<Item = (&'m (i32, i32, i32), &'m Vec<ChunkMesh>)>,
    planes: &[[f32; 4]; 6],
) -> (Vec<DrawIndexedIndirectArgs>, u32, u32) {
    let mut draws: Vec<DrawIndexedIndirectArgs> = Vec::new();
    let mut visible: u32 = 0;
    let mut culled: u32 = 0;
    for (key, meshes) in sections {
        let min = [key.0 as f32 * 16.0, key.1 as f32 * 16.0, key.2 as f32 * 16.0];
        let max = [min[0] + 16.0, min[1] + 16.0, min[2] + 16.0];
        if !aabb_in_frustum(min, max, planes) {
            culled += 1;
            continue;
        }
        visible += 1;
        for m in meshes {
            draws.push(DrawIndexedIndirectArgs {
                index_count: m.index_count,
                instance_count: 1,
                first_index: m.index_offset,
                base_vertex: m.base_vertex as i32,
                first_instance: 0,
            });
        }
    }
    (draws, visible, culled)
}

fn make_depth_texture(device: &wgpu::Device, width: u32, height: u32) -> wgpu::Texture {
    device.create_texture(&wgpu::TextureDescriptor {
        label: Some("Depth Texture"),
        size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Depth32Float,
        usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
        view_formats: &[],
    })
}

fn make_color_texture(device: &wgpu::Device, width: u32, height: u32) -> wgpu::Texture {
    device.create_texture(&wgpu::TextureDescriptor {
        label: Some("Color Texture"),
        size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Rgba8UnormSrgb,
        usage: wgpu::TextureUsages::RENDER_ATTACHMENT
            | wgpu::TextureUsages::COPY_SRC
            | wgpu::TextureUsages::TEXTURE_BINDING,
        view_formats: &[],
    })
}

fn make_staging_buffer(device: &wgpu::Device, width: u32, height: u32) -> wgpu::Buffer {
    let row_aligned = ((width * 4 + 255) / 256) * 256;
    device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Staging"),
        size: (row_aligned as u64) * (height as u64),
        usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
        mapped_at_creation: false,
    })
}

fn aligned_row(width: u32) -> u32 {
    ((width * 4 + 255) / 256) * 256
}

/// Generate a sky dome mesh (upper hemisphere) for gradient sky rendering.
/// Returns (vertices, indices) with positions on a 500-unit sphere surface.
/// Vertex count: (LAT+1)*(LON+1) = 17*33 = 561
/// Index count: LAT*LON*6 = 16*32*6 = 3072
fn create_sky_dome_mesh() -> (Vec<SkyVertex>, Vec<u16>) {
    const LAT: u32 = 16;
    const LON: u32 = 32;
    const RADIUS: f32 = 500.0;

    let lat1 = LAT + 1;
    let lon1 = LON + 1;
    let mut verts = Vec::with_capacity((lat1 * lon1) as usize);
    let mut idxs = Vec::with_capacity((LAT * LON * 6) as usize);

    for lat in 0..lat1 {
        let theta = (lat as f32 / LAT as f32) * (std::f32::consts::PI / 2.0);
        let (st, ct) = (theta.sin(), theta.cos());
        for lon in 0..lon1 {
            let phi = (lon as f32 / LON as f32) * 2.0 * std::f32::consts::PI;
            let (sp, cp) = (phi.sin(), phi.cos());
            let nx = st * cp;
            let ny = ct;
            let nz = st * sp;
            verts.push(SkyVertex {
                position: [nx * RADIUS, ny * RADIUS, nz * RADIUS],
                normal: [nx, ny, nz],
            });
        }
    }

    for lat in 0..LAT {
        for lon in 0..LON {
            let a = lat * lon1 + lon;
            let b = a + lon1;
            idxs.extend_from_slice(&[a as u16, b as u16, (a + 1) as u16]);
            idxs.extend_from_slice(&[b as u16, (b + 1) as u16, (a + 1) as u16]);
        }
    }

    (verts, idxs)
}

fn create_plane_mesh(device: &wgpu::Device, size: f32, y: f32, z_center: f32, color: [f32; 3])
    -> (wgpu::Buffer, wgpu::Buffer, u32)
{
    let h = size * 0.5;
    let z0 = z_center - h;
    let z1 = z_center + h;
    let vertices: [Vertex; 4] = [
        Vertex { position: [-h, y, z0], color },
        Vertex { position: [ h, y, z0], color },
        Vertex { position: [-h, y, z1], color },
        Vertex { position: [ h, y, z1], color },
    ];
    let indices: [u16; 6] = [0, 3, 1, 0, 2, 3];  // CCW → +Y normal

    let vbuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Plane VB"),
        size: std::mem::size_of_val(&vertices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::VERTEX,
        mapped_at_creation: true,
    });
    vbuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&vertices));
    vbuf.unmap();

    let ibuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Plane IB"),
        size: std::mem::size_of_val(&indices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::INDEX,
        mapped_at_creation: true,
    });
    ibuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&indices));
    ibuf.unmap();

    (vbuf, ibuf, indices.len() as u32)
}

/// Create a cube mesh with vertices pre-offsetted by (ox, oy, oz).
/// Shares a single index buffer for all cubes.
fn create_cube_mesh_at(
    device: &wgpu::Device,
    color: [f32; 3],
    offset: (f32, f32, f32),
) -> wgpu::Buffer {
    let c = color;
    let d = [c[0] * 0.6, c[1] * 0.6, c[2] * 0.6];
    let (ox, oy, oz) = offset;
    let vertices: [Vertex; 24] = [
        Vertex { position: [-0.5+ox,  0.5+oy, -0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy, -0.5+oz], color: c },
        Vertex { position: [-0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox, -0.5+oy,  0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy,  0.5+oz], color: d },
        Vertex { position: [-0.5+ox, -0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox, -0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox,  0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox,  0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy, -0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy, -0.5+oz], color: c },
        Vertex { position: [ 0.5+ox, -0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox,  0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox, -0.5+oy,  0.5+oz], color: d },
        Vertex { position: [-0.5+ox,  0.5+oy,  0.5+oz], color: d },
    ];

    let vbuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Cube VB"),
        size: std::mem::size_of_val(&vertices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::VERTEX,
        mapped_at_creation: true,
    });
    vbuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&vertices));
    vbuf.unmap();
    vbuf
}

// ── Create wgpu Surface from Windows HWND ─────────────────────────

fn create_surface_from_hwnd(
    instance: &wgpu::Instance,
    hwnd: usize,
) -> Option<wgpu::Surface<'static>> {
    use raw_window_handle::{
        RawDisplayHandle, RawWindowHandle, WindowsDisplayHandle, Win32WindowHandle,
    };

    let hwnd_isize = hwnd as isize;
    let raw_handle = RawWindowHandle::Win32(
        Win32WindowHandle::new(std::num::NonZeroIsize::new(hwnd_isize)?)
    );
    let display_handle = RawDisplayHandle::Windows(WindowsDisplayHandle::new());

    let surface = unsafe {
        instance.create_surface_unsafe(
            wgpu::SurfaceTargetUnsafe::RawHandle {
                raw_window_handle: raw_handle,
                raw_display_handle: display_handle,
            }
        )
    };
    match surface {
        Ok(s) => Some(s),
        Err(e) => {
            log::error!("[dx12-wm] create_surface_unsafe failed: {:?}", e);
            None
        }
    }
}

// ── Chunk mesh storage (MC section geometry → D3D12) ─────────────

/// One mesh = one RenderLayer of one 16×16×16 chunk section.
/// Keyed by (section_x, section_y, section_z).
///
/// Phase 11e: vertex/index data is kept on the CPU and merged into two
/// big GPU buffers by `rebuild_chunk_buffers()`. The `base_vertex` /
/// `index_offset` / `index_count` fields locate this mesh inside those
/// merged buffers.
struct ChunkMesh {
    /// CPU copy of converted vertices (world-space ChunkVertex).
    vertices: Vec<ChunkVertex>,
    /// CPU copy of triangle indices (always u32 so the merged index buffer
    /// has a single format).
    indices: Vec<u32>,
    /// First vertex of this mesh in the merged vertex buffer.
    base_vertex: u32,
    /// First index of this mesh in the merged index buffer.
    index_offset: u32,
    /// Number of indices (6 per quad).
    index_count: u32,
}

/// One entry of a draw-indexed-indirect argument buffer.
/// Layout must exactly match D3D12_DRAW_INDEXED_ARGUMENTS (20 bytes,
/// little-endian): IndexCountPerInstance, InstanceCount, StartIndexLocation,
/// BaseVertexLocation, StartInstanceLocation.
#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct DrawIndexedIndirectArgs {
    index_count: u32,
    instance_count: u32,
    first_index: u32,
    base_vertex: i32,
    first_instance: u32,
}

// ── Ring slot ─────────────────────────────────────────────────────

struct Slot {
    #[allow(dead_code)]
    color: wgpu::Texture,
    #[allow(dead_code)]
    depth: wgpu::Texture,
    depth_view: wgpu::TextureView,
    staging: wgpu::Buffer,
}

impl Slot {
    fn new(device: &wgpu::Device, width: u32, height: u32) -> Self {
        let color = make_color_texture(device, width, height);
        let depth = make_depth_texture(device, width, height);
        let depth_view = depth.create_view(&wgpu::TextureViewDescriptor::default());
        let staging = make_staging_buffer(device, width, height);
        Self { color, depth, depth_view, staging }
    }
}

// ████████████████████████████████████████████████████████████████████████
// ██  RENDERER                                                       ██
// ████████████████████████████████████████████████████████████████████████

pub struct WmRenderer {
    #[allow(dead_code)]
    instance: wgpu::Instance,
    adapter: wgpu::Adapter,
    device: wgpu::Device,
    queue: wgpu::Queue,

    width: u32,
    height: u32,

    pub camera_mvp: [[f32; 4]; 4],
    camera_prev: [[f32; 4]; 4],
    camera_target: [[f32; 4]; 4],
    camera_pos: [f32; 3],
    fog_color: [f32; 4], // rgb + density
    sky_color: [f32; 3], // zenith sky color for sky dome

    // Immutable GPU resources
    pipeline: wgpu::RenderPipeline,
    bind_group: wgpu::BindGroup,
    uniform_buffer: wgpu::Buffer,
    plane_vb: wgpu::Buffer,
    plane_ib: wgpu::Buffer,
    plane_count: u32,
    cube_vbs: Vec<wgpu::Buffer>,    // One VB per cube position
    cube_ib: wgpu::Buffer,          // Shared index buffer
    cube_count: u32,

    // Surface mode (native swapchain, no readback)
    surface: Option<wgpu::Surface<'static>>,
    surface_config: Option<wgpu::SurfaceConfiguration>,
    surface_format: wgpu::TextureFormat,
    surface_depth: Option<wgpu::Texture>,  // Cached depth texture (reused per-frame)
    surface_hwnd: usize,  // Track HWND to detect fullscreen/resize transitions
    /// True when a window resize has been received but the swapchain has not
    /// been reconfigured yet. Forces a surface reconfig in render_surface().
    resize_pending: bool,

    // Textured fullscreen quad (GL framebuffer → D3D12 display)
    tex_pipeline: wgpu::RenderPipeline,
    tex_bind_group: Option<wgpu::BindGroup>,
    tex_sampler: wgpu::Sampler,
    frame_texture: Option<wgpu::Texture>,
    frame_width: u32,
    frame_height: u32,
    fs_quad_vb: wgpu::Buffer,
    fs_quad_ib: wgpu::Buffer,

    // HUD overlay (GL-rendered UI composited on top of D3D12 world)
    hud_pipeline: Option<wgpu::RenderPipeline>,
    hud_texture: Option<wgpu::Texture>,
    hud_bind_group: Option<wgpu::BindGroup>,
    hud_width: u32,
    hud_height: u32,

    // Offscreen mode (triple-buffer readback)
    slots: [Slot; RING_SIZE],
    idx: usize,
    pending_rx: [Option<mpsc::Receiver<Result<(), wgpu::BufferAsyncError>>>; RING_SIZE],
    prev_pixels: Vec<u8>,

    // Chunk geometry (Phase 7: native MC geometry → D3D12)
    chunk_meshes: std::collections::HashMap<(i32, i32, i32), Vec<ChunkMesh>>,
    has_chunk_geometry: bool,

    // Phase 11e: batched chunk rendering — merged VB/IB + indirect draws
    chunk_vb: Option<wgpu::Buffer>,       // merged vertex buffer (all meshes)
    chunk_ib: Option<wgpu::Buffer>,       // merged index buffer (u32)
    chunk_indirect: Option<wgpu::Buffer>, // draw-indexed-indirect args buffer
    chunk_indirect_capacity: u32,         // entries the indirect buffer can hold
    chunk_geometry_dirty: bool,           // merged buffers need rebuilding
    chunk_batch_enabled: bool,            // false → per-mesh draw_indexed fallback

    // Phase 11j: incremental chunk merging — CPU-side flat copies of the
    // merged VB/IB data plus the capacity each GPU buffer was allocated for.
    // New section uploads append into these (O(1) per mesh, no full rebuild);
    // a full rebuild only happens when a section is cleared (recompile/unload),
    // which is far rarer than the per-frame upload storm during world loading.
    merged_verts: Vec<ChunkVertex>,
    merged_indices: Vec<u32>,
    chunk_vb_capacity: u64, // allocated vertex bytes in chunk_vb
    chunk_ib_capacity: u64, // allocated index bytes in chunk_ib
    chunk_need_full_rebuild: bool, // true → rebuild_chunk_buffers() required

    // Chunk textured pipeline + atlas
    chunk_shader: Option<wgpu::ShaderModule>,  // stored for lazy pipeline creation
    chunk_pipeline: Option<wgpu::RenderPipeline>,
    chunk_pipeline_transparent: Option<wgpu::RenderPipeline>,
    chunk_bind_group: Option<wgpu::BindGroup>,
    chunk_bind_group_layout: Option<wgpu::BindGroupLayout>,
    atlas_texture: Option<wgpu::Texture>,
    atlas_sampler: wgpu::Sampler,
    atlas_width: u32,
    atlas_height: u32,
    /// Raw atlas pixels stored for diagnostics
    atlas_pixels: Option<Vec<u8>>,

    // Lightmap texture (dynamic block/sky lighting)
    lightmap_texture: Option<wgpu::Texture>,
    lightmap_sampler: wgpu::Sampler,
    lightmap_width: u32,
    lightmap_height: u32,

    // Entity rendering (Phase 7 task 1: colored boxes at entity positions)
    entity_buffer: Option<wgpu::Buffer>,
    entity_count: u32,
    entity_pipeline: Option<wgpu::RenderPipeline>,

    // Phase 11g: last-uploaded data for change detection (skip redundant uploads)
    last_entity_data: Vec<f32>,
    entity_uploads: u64,

    // Particle rendering (Phase 7 task 2: point sprites)
    particle_buffer: Option<wgpu::Buffer>,
    particle_count: u32,
    particle_pipeline: Option<wgpu::RenderPipeline>,
    last_particle_data: Vec<f32>,
    particle_uploads: u64,

    // Sky dome rendering
    sky_vb: Option<wgpu::Buffer>,
    sky_ib: Option<wgpu::Buffer>,
    sky_index_count: u32,
    sky_pipeline: Option<wgpu::RenderPipeline>,
}

// SAFETY: WmRenderer is only accessed from the JNI thread (Minecraft render thread).
// The Surface is !Send (tied to window system), but we never send it across threads.
unsafe impl Send for WmRenderer {}

impl WmRenderer {
    pub fn create(width: u32, height: u32) -> Result<Self, &'static str> {
        // Print the build identity first: the JAR-internal DLL overwrites any
        // external copy on every launch, so this hash is the only reliable way
        // to confirm which wgpu_mc_jni.dll build is actually running.
        let build_id = option_env!("GIT_COMMIT_HASH").unwrap_or("unknown");
        eprintln!("[dx12-wm] wgpu-mc build {} (v{})", build_id, env!("CARGO_PKG_VERSION"));
        eprintln!("[dx12-wm] Creating WmRenderer {}x{} (triple-buffer + surface support)", width, height);
        log::info!("Creating WmRenderer {}x{}", width, height);

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::DX12,
            ..Default::default()
        });

        let adapter = futures::executor::block_on(instance.request_adapter(
            &wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: None,
                ..Default::default()
            },
        ))
        .ok_or("No adapter")?;
        eprintln!("[dx12-wm] Adapter: {:?}", adapter.get_info().name);

        // Phase 11e: MULTI_DRAW_INDIRECT lets us render all visible chunk
        // meshes with a single multi_draw_indexed_indirect call per pass.
        // D3D12 supports it natively (confirmed in wgpu-hal dx12/adapter.rs);
        // if an adapter ever lacks it we fall back to per-mesh draw_indexed
        // from the merged buffers.
        let chunk_batch_enabled = adapter
            .features()
            .contains(wgpu::Features::MULTI_DRAW_INDIRECT);
        if !chunk_batch_enabled {
            eprintln!("[dx12-wm] WARN: MULTI_DRAW_INDIRECT unsupported — using per-mesh draw fallback");
        }
        let required_features = if chunk_batch_enabled {
            wgpu::Features::MULTI_DRAW_INDIRECT
        } else {
            wgpu::Features::empty()
        };

        let (device, queue) = futures::executor::block_on(adapter.request_device(
            &wgpu::DeviceDescriptor {
                label: Some("wgpu-mc"),
                required_features,
                required_limits: wgpu::Limits::default(),
                memory_hints: Default::default(),
            },
            None,
        ))
        .map_err(|_| "Device failed")?;
        eprintln!("[dx12-wm] Device created OK");

        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Main Shader"),
            source: wgpu::ShaderSource::Wgsl(std::borrow::Cow::Borrowed(SHADER_SRC)),
        });

        let uniform_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Camera Uniform"),
            size: 128, // mat4x4 (64) + camera_pos (16) + fog (16) + sky_top (16)
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        let bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Camera Bind Group Layout"),
            entries: &[wgpu::BindGroupLayoutEntry {
                binding: 0,
                visibility: wgpu::ShaderStages::VERTEX,
                ty: wgpu::BindingType::Buffer {
                    ty: wgpu::BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size: None,
                },
                count: None,
            }],
        });

        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Camera Bind Group"),
            layout: &bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: uniform_buffer.as_entire_binding(),
            }],
        });

        // No push constant ranges — compatible with all GPUs
        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Pipeline Layout"),
            bind_group_layouts: &[&bind_group_layout],
            push_constant_ranges: &[],
        });

        let pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Main Pipeline"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: Some("vs_main"),
                compilation_options: Default::default(),
                buffers: &[Vertex::desc()],
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: Some("fs_main"),
                compilation_options: Default::default(),
                targets: &[Some(wgpu::ColorTargetState {
                    format: wgpu::TextureFormat::Rgba8UnormSrgb,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: Some(wgpu::Face::Back),
                ..Default::default()
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: wgpu::TextureFormat::Depth32Float,
                depth_write_enabled: true,
                depth_compare: wgpu::CompareFunction::Less,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });

        let (plane_vb, plane_ib, plane_count) =
            create_plane_mesh(&device, 20.0, -4.0, 8.0, [0.2, 0.65, 0.2]);

        // Create one cube VB per position (model offsets baked into vertices).
        // Positions are RELATIVE to camera (camera_pos added in shader).
        // y = -3.5: on the ground plane; spread over xz for visibility.
        let cube_positions: [([f32; 3], [f32; 3]); 5] = [
            ([-5.0, -3.5,  5.0], [0.9, 0.3, 0.15]),  // front-left, orange
            ([ 5.0, -3.5,  5.0], [0.15, 0.5, 0.9]),  // front-right, blue
            ([ 0.0, -1.0,  3.0], [0.9, 0.7, 0.1]),   // center, yellow
            ([-5.0, -3.5, 10.0], [0.4, 0.8, 0.2]),   // far front-left, green
            ([ 5.0, -3.5, 10.0], [0.7, 0.2, 0.5]),   // far front-right, magenta
        ];

        // Shared index buffer — all cubes use identical index data
        let cube_indices: [u16; 36] = [
             0,  1,  2,  2,  1,  3,
             4,  6,  5,  5,  6,  7,
             8,  9, 10, 10,  9, 11,
            12, 14, 13, 13, 14, 15,
            16, 17, 18, 18, 17, 19,
            20, 22, 21, 21, 22, 23,
        ];
        let cube_count = cube_indices.len() as u32;
        let cube_ib = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Cube IB (shared)"),
            size: std::mem::size_of_val(&cube_indices) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::INDEX,
            mapped_at_creation: true,
        });
        cube_ib.slice(..).get_mapped_range_mut()[..]
            .copy_from_slice(bytemuck::cast_slice(&cube_indices));
        cube_ib.unmap();

        let mut cube_vbs = Vec::with_capacity(cube_positions.len());
        for &(pos, color) in &cube_positions {
            cube_vbs.push(create_cube_mesh_at(&device, color, (pos[0], pos[1], pos[2])));
        }

        // ---- Textured fullscreen quad pipeline (GL framebuffer → D3D12) ----
        let tex_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Texture Shader"),
            source: wgpu::ShaderSource::Wgsl(TEX_SHADER_SRC.into()),
        });

        let tex_bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Texture BGL"),
            entries: &[
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Texture {
                        sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        view_dimension: wgpu::TextureViewDimension::D2,
                        multisampled: false,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 1,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                    count: None,
                },
            ],
        });

        let tex_pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Texture Pipeline Layout"),
            bind_group_layouts: &[&tex_bind_group_layout],
            push_constant_ranges: &[],
        });

        let tex_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Texture Pipeline"),
            layout: Some(&tex_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &tex_shader,
                entry_point: Some("vs_main"),
                buffers: &[TexVertex::desc()],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &tex_shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: wgpu::TextureFormat::Rgba8UnormSrgb,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: None,
                ..Default::default()
            },
            depth_stencil: None,
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });

        let tex_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
            label: Some("Texture Sampler"),
            address_mode_u: wgpu::AddressMode::ClampToEdge,
            address_mode_v: wgpu::AddressMode::ClampToEdge,
            address_mode_w: wgpu::AddressMode::ClampToEdge,
            mag_filter: wgpu::FilterMode::Nearest,
            min_filter: wgpu::FilterMode::Nearest,
            ..Default::default()
        });

        // Fullscreen quad (two triangles covering NDC [-1,1]²)
        let quad_vertices: [TexVertex; 4] = [
            TexVertex { position: [-1.0, -1.0], uv: [0.0, 1.0] },
            TexVertex { position: [ 1.0, -1.0], uv: [1.0, 1.0] },
            TexVertex { position: [-1.0,  1.0], uv: [0.0, 0.0] },
            TexVertex { position: [ 1.0,  1.0], uv: [1.0, 0.0] },
        ];
        let quad_indices: [u16; 6] = [0, 1, 2, 1, 3, 2];

        let fs_quad_vb = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("FS Quad VB"),
            size: std::mem::size_of_val(&quad_vertices) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::VERTEX,
            mapped_at_creation: true,
        });
        fs_quad_vb.slice(..).get_mapped_range_mut()[..]
            .copy_from_slice(bytemuck::cast_slice(&quad_vertices));
        fs_quad_vb.unmap();

        let fs_quad_ib = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("FS Quad IB"),
            size: std::mem::size_of_val(&quad_indices) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::INDEX,
            mapped_at_creation: true,
        });
        fs_quad_ib.slice(..).get_mapped_range_mut()[..]
            .copy_from_slice(bytemuck::cast_slice(&quad_indices));
        fs_quad_ib.unmap();

        let slots = [
            Slot::new(&device, width, height),
            Slot::new(&device, width, height),
            Slot::new(&device, width, height),
        ];

        eprintln!("[dx12-wm] Offscreen slots created ({}x{})", width, height);

        // Chunk atlas sampler: use Linear filtering for smooth block textures
        let atlas_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
            label: Some("Atlas Sampler"),
            address_mode_u: wgpu::AddressMode::ClampToEdge,
            address_mode_v: wgpu::AddressMode::ClampToEdge,
            address_mode_w: wgpu::AddressMode::ClampToEdge,
            mag_filter: wgpu::FilterMode::Nearest,
            min_filter: wgpu::FilterMode::Nearest,
            ..Default::default()
        });

        // Create a 1x1 white texture as default lightmap (mid-level light).
        let default_lightmap = device.create_texture(&wgpu::TextureDescriptor {
            label: Some("Default Lightmap (1x1 white)"),
            size: wgpu::Extent3d { width: 1, height: 1, depth_or_array_layers: 1 },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8UnormSrgb,
            usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
            view_formats: &[],
        });
        queue.write_texture(
            wgpu::ImageCopyTexture {
                texture: &default_lightmap,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            &[255u8, 255, 255, 255], // white = full brightness
            wgpu::ImageDataLayout {
                offset: 0,
                bytes_per_row: Some(4),
                rows_per_image: Some(1),
            },
            wgpu::Extent3d { width: 1, height: 1, depth_or_array_layers: 1 },
        );
        let default_lightmap_view = default_lightmap.create_view(&wgpu::TextureViewDescriptor::default());

        let lightmap_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
            label: Some("Lightmap Sampler"),
            address_mode_u: wgpu::AddressMode::ClampToEdge,
            address_mode_v: wgpu::AddressMode::ClampToEdge,
            address_mode_w: wgpu::AddressMode::ClampToEdge,
            mag_filter: wgpu::FilterMode::Nearest,
            min_filter: wgpu::FilterMode::Nearest,
            ..Default::default()
        });

        // Chunk bind group layout: camera uniform + atlas texture + atlas sampler + lightmap texture + lightmap sampler
        let chunk_bgl = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Chunk BGL"),
            entries: &[
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::VERTEX | wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 1,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Texture {
                        sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        view_dimension: wgpu::TextureViewDimension::D2,
                        multisampled: false,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 2,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 3,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Texture {
                        sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        view_dimension: wgpu::TextureViewDimension::D2,
                        multisampled: false,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 4,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                    count: None,
                },
            ],
        });

        // Create a default chunk bind group with the 1x1 white lightmap.
        // This is replaced when upload_terrain_atlas() is called with the real atlas.
        // The default bind group uses the uniform buffer + dummy atlas/uniform for completeness,
        // but will be replaced once the real atlas is uploaded.
        // For now, only lightmap is set up correctly; chunks won't render until atlas arrives.
        // We need a temporary bind group so that entity/particle/sky setups that share the
        // chunk BGL can work even before atlas upload.
        //
        // Create a 1x1 fallback atlas texture for the bind group (gray pixel).
        let fallback_atlas = device.create_texture(&wgpu::TextureDescriptor {
            label: Some("Fallback Atlas (1x1 gray)"),
            size: wgpu::Extent3d { width: 1, height: 1, depth_or_array_layers: 1 },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8UnormSrgb,
            usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
            view_formats: &[],
        });
        queue.write_texture(
            wgpu::ImageCopyTexture {
                texture: &fallback_atlas,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            &[128u8, 128, 128, 255], // gray
            wgpu::ImageDataLayout {
                offset: 0,
                bytes_per_row: Some(4),
                rows_per_image: Some(1),
            },
            wgpu::Extent3d { width: 1, height: 1, depth_or_array_layers: 1 },
        );
        let fallback_atlas_view = fallback_atlas.create_view(&wgpu::TextureViewDescriptor::default());

        let default_chunk_bg = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Default Chunk Bind Group (fallback)"),
            layout: &chunk_bgl,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: uniform_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: wgpu::BindingResource::TextureView(&fallback_atlas_view),
                },
                wgpu::BindGroupEntry {
                    binding: 2,
                    resource: wgpu::BindingResource::Sampler(&atlas_sampler),
                },
                wgpu::BindGroupEntry {
                    binding: 3,
                    resource: wgpu::BindingResource::TextureView(&default_lightmap_view),
                },
                wgpu::BindGroupEntry {
                    binding: 4,
                    resource: wgpu::BindingResource::Sampler(&lightmap_sampler),
                },
            ],
        });

        let _chunk_pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Chunk Pipeline Layout"),
            bind_group_layouts: &[&chunk_bgl],
            push_constant_ranges: &[],
        });

        eprintln!("[dx12-wm] Chunk pipeline layout created (texture atlas + lightmap support ready)");

        Ok(Self {
            instance,
            adapter,
            device,
            queue,
            width,
            height,
            camera_mvp: IDENTITY,
            camera_prev: IDENTITY,
            camera_target: IDENTITY,
            camera_pos: [0.0; 3],
            fog_color: [1.0, 0.0, 0.0, 0.001], // DIAG: bright red fog to verify shader mixing
            sky_color: [0.4, 0.6, 0.9], // default sky blue zenith
            pipeline,
            bind_group,
            uniform_buffer,
            plane_vb,
            plane_ib,
            plane_count,
            cube_vbs,
            cube_ib,
            cube_count,
            surface: None,
            surface_config: None,
            surface_format: wgpu::TextureFormat::Bgra8UnormSrgb,
            surface_depth: None,
            surface_hwnd: 0,
            resize_pending: false,
            tex_pipeline,
            tex_bind_group: None,
            tex_sampler,
            frame_texture: None,
            frame_width: 0,
            frame_height: 0,
            fs_quad_vb,
            fs_quad_ib,
            hud_pipeline: None,
            hud_texture: None,
            hud_bind_group: None,
            hud_width: 0,
            hud_height: 0,
            slots,
            idx: 0,
            pending_rx: [None, None, None],
            prev_pixels: Vec::new(),
            chunk_meshes: std::collections::HashMap::new(),
            has_chunk_geometry: false,
            chunk_vb: None,
            chunk_ib: None,
            chunk_indirect: None,
            chunk_indirect_capacity: 0,
            chunk_geometry_dirty: false,
            chunk_batch_enabled: chunk_batch_enabled,
            merged_verts: Vec::new(),
            merged_indices: Vec::new(),
            chunk_vb_capacity: 0,
            chunk_ib_capacity: 0,
            chunk_need_full_rebuild: false,
            chunk_shader: None,
            chunk_pipeline: None,
            chunk_pipeline_transparent: None,
            chunk_bind_group: Some(default_chunk_bg),
            chunk_bind_group_layout: Some(chunk_bgl),
            // Store fallback atlas here initially — the default chunk bind group
            // references this texture's view, so it must outlive the bind group.
            // Replaced by upload_terrain_atlas() once the real atlas arrives.
            atlas_texture: Some(fallback_atlas),
            atlas_sampler,
            atlas_width: 0,
            atlas_height: 0,
            atlas_pixels: None,

            // Lightmap: starts with a 1x1 white fallback texture
            lightmap_texture: Some(default_lightmap),
            lightmap_sampler,
            lightmap_width: 1,
            lightmap_height: 1,

            // Entity + particle rendering (created lazily)
            entity_buffer: None,
            entity_count: 0,
            entity_pipeline: None,
            particle_buffer: None,
            particle_count: 0,
            particle_pipeline: None,
            last_entity_data: Vec::new(),
            entity_uploads: 0,
            last_particle_data: Vec::new(),
            particle_uploads: 0,

            // Sky dome (created lazily)
            sky_vb: None,
            sky_ib: None,
            sky_index_count: 0,
            sky_pipeline: None,
        })
    }

    /// Initialize a D3D12 swapchain surface on the given HWND.
    ///
    /// To avoid GPU TDR, the GL context MUST be temporarily detached from the HWND
    /// before calling renderFrame() (via GLFW.glfwMakeContextCurrent(0) on the Java
    /// side) and reattached after. This ensures D3D12's Present() has exclusive
    /// access to the HWND, preventing WDDM driver contention.
    pub fn init_surface(&mut self, hwnd: usize) {
        // If the HWND changed (fullscreen toggle, window resize, etc.),
        // destroy the old surface and create a new one on the new HWND.
        if self.surface.is_some() {
            if self.surface_hwnd == hwnd {
                return; // Same HWND, surface already exists
            }
            // HWND changed — drop old surface first so a new one is created
            log::info!("[dx12-wm] HWND changed 0x{:x} → 0x{:x}, recreating surface",
                self.surface_hwnd, hwnd);
            self.surface = None;
            self.surface_depth = None;
            self.surface_config = None;
            self.surface_hwnd = 0;
        }

        log::info!("[dx12-wm] init_surface: creating D3D12 swapchain on HWND 0x{:x} (parent HWND)",
            hwnd);

        let surface = match create_surface_from_hwnd(&self.instance, hwnd) {
            Some(s) => s,
            None => {
                log::error!("[dx12-wm] Failed to create wgpu surface from HWND 0x{:x}", hwnd);
                return;
            }
        };

        let caps = surface.get_capabilities(&self.adapter);
        // Prefer Rgba8UnormSrgb to match the pipeline format.
        // If not available, fall back to any sRGB format.
        let format = caps.formats.iter()
            .find(|f| **f == wgpu::TextureFormat::Rgba8UnormSrgb)
            .or_else(|| caps.formats.iter().find(|f| f.is_srgb()))
            .copied()
            .unwrap_or(caps.formats[0]);

        log::info!("[dx12-wm] Surface caps: format={:?}, {}x{}, present_modes={:?}",
            format, self.width, self.height, caps.present_modes);

        let present_mode = if caps.present_modes.contains(&wgpu::PresentMode::Immediate) {
            wgpu::PresentMode::Immediate
        } else if caps.present_modes.contains(&wgpu::PresentMode::Fifo) {
            wgpu::PresentMode::Fifo
        } else {
            caps.present_modes[0]
        };

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format,
            width: self.width,
            height: self.height,
            present_mode,
            alpha_mode: wgpu::CompositeAlphaMode::Opaque,
            view_formats: vec![],
            desired_maximum_frame_latency: 1,
        };

        surface.configure(&self.device, &config);

        // Create and cache depth texture (reused every frame)
        self.surface_depth = Some(make_depth_texture(&self.device, self.width, self.height));

        self.surface_format = format;
        self.surface_config = Some(config);
        self.surface = Some(surface);
        self.surface_hwnd = hwnd;

        // Rebuild chunk pipelines with the actual surface format if atlas already uploaded.
        // Drop the old pipelines first — they may have been created with the wrong format.
        self.chunk_pipeline = None;
        self.chunk_pipeline_transparent = None;
        self.entity_pipeline = None;
        self.particle_pipeline = None;
        self.sky_pipeline = None;      // Recreated on next render with new surface format
        self.hud_pipeline = None;      // Will be recreated on next set_hud_pixels call
        self.hud_bind_group = None;    // Also invalidate bind group — has_hud checks this
        self.hud_texture = None;       // Texture must be rebuilt with new surface config
        self.ensure_chunk_pipeline();

        log::info!("[dx12-wm] Surface mode ENABLED on parent HWND: {:?} {}x{}",
            format, self.width, self.height);
        eprintln!("[dx12-wm] Surface mode ENABLED: {:?} {}x{}", format, self.width, self.height);
    }

    /// Returns true if the renderer has an active surface (swapchain mode).
    pub fn has_surface(&self) -> bool {
        self.surface.is_some()
    }

    /// Returns true if any MC chunk geometry has been uploaded.
    pub fn has_chunk_geometry(&self) -> bool {
        self.has_chunk_geometry
    }

    /// Upload MC terrain atlas texture for chunk rendering.
    /// `pixels` is RGBA8 data, width and height are atlas dimensions.
    pub fn upload_terrain_atlas(&mut self, pixels: &[u8], width: u32, height: u32) {
        if pixels.len() < (width * height * 4) as usize { return; }

        log::info!("[dx12-wm] Uploading terrain atlas: {}x{} ({} bytes)",
            width, height, pixels.len());

        self.atlas_width = width;
        self.atlas_height = height;
        self.atlas_pixels = Some(pixels.to_vec());  // stored for diagnostics

        // Generate full mip chain on CPU for proper LOD behavior.
        let mip_count = (width.max(height) as f64).log2().floor() as u32 + 1;
        let mut mip_data: Vec<Vec<u8>> = Vec::with_capacity(mip_count as usize);
        mip_data.push(pixels.to_vec());
        let mut mw = width;
        let mut mh = height;
        for _ in 1..mip_count {
            let nw = (mw / 2).max(1);
            let nh = (mh / 2).max(1);
            let prev = mip_data.last().unwrap();
            let mut level = vec![0u8; (nw * nh * 4) as usize];
            for y in 0..nh {
                for x in 0..nw {
                    let mut rgba = [0u32; 4];
                    let mut count = 0u32;
                    for dy in 0..2 {
                        for dx in 0..2 {
                            let px = (x * 2 + dx).min(mw - 1);
                            let py = (y * 2 + dy).min(mh - 1);
                            let idx = ((py * mw + px) * 4) as usize;
                            for c in 0..4 { rgba[c] += prev[idx + c] as u32; }
                            count += 1;
                        }
                    }
                    let idx = ((y * nw + x) * 4) as usize;
                    for c in 0..4 { level[idx + c] = (rgba[c] / count) as u8; }
                }
            }
            mip_data.push(level);
            mw = nw;
            mh = nh;
        }
        log::info!("[dx12-wm] Generated {} mip levels for terrain atlas", mip_count);

        let atlas = self.device.create_texture(&wgpu::TextureDescriptor {
            label: Some("Terrain Atlas"),
            size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
            mip_level_count: mip_count,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8UnormSrgb,
            usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
            view_formats: &[],
        });

        for (i, mip) in mip_data.iter().enumerate() {
            let lod_w = (width >> i).max(1);
            let lod_h = (height >> i).max(1);
            self.queue.write_texture(
                wgpu::ImageCopyTexture {
                    texture: &atlas,
                    mip_level: i as u32,
                    origin: wgpu::Origin3d::ZERO,
                    aspect: wgpu::TextureAspect::All,
                },
                mip,
                wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(lod_w * 4),
                    rows_per_image: Some(lod_h),
                },
                wgpu::Extent3d { width: lod_w, height: lod_h, depth_or_array_layers: 1 },
            );
        }

        // Diagnostic: verify first few pixels of the uploaded atlas
        if pixels.len() >= 16 {
            let r = pixels[0]; let g = pixels[1]; let b = pixels[2]; let a = pixels[3];
            eprintln!("[dx12-wm] Atlas first pixel: RGBA=({},{},{},{})", r, g, b, a);
        }
        eprintln!("[dx12-wm] Atlas texture uploaded: {}x{} ({:.1} MB)", width, height, pixels.len() as f64 / 1048576.0);

        // Save atlas as PNG for visual debugging (open in Photoshop/GIMP to inspect texture positions)
        let atlas_path = std::path::Path::new("atlas_debug.png");
        if let Err(e) = image::save_buffer(atlas_path, pixels, width, height, image::ColorType::Rgba8) {
            log::warn!("[dx12-wm] Failed to save atlas PNG: {}", e);
        } else {
            log::info!("[dx12-wm] Atlas saved to atlas_debug.png ({}x{})", width, height);
            eprintln!("[dx12-wm] Atlas saved to atlas_debug.png ({}x{})", width, height);
        }

        // Create the chunk bind group (now includes lightmap)
        let atlas_view = atlas.create_view(&wgpu::TextureViewDescriptor::default());

        let chunk_bgl = self.chunk_bind_group_layout.as_ref().unwrap();
        let lightmap_view = self.lightmap_texture.as_ref()
            .unwrap()
            .create_view(&wgpu::TextureViewDescriptor::default());
        let chunk_bg = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Chunk Bind Group"),
            layout: chunk_bgl,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: self.uniform_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: wgpu::BindingResource::TextureView(&atlas_view),
                },
                wgpu::BindGroupEntry {
                    binding: 2,
                    resource: wgpu::BindingResource::Sampler(&self.atlas_sampler),
                },
                wgpu::BindGroupEntry {
                    binding: 3,
                    resource: wgpu::BindingResource::TextureView(&lightmap_view),
                },
                wgpu::BindGroupEntry {
                    binding: 4,
                    resource: wgpu::BindingResource::Sampler(&self.lightmap_sampler),
                },
            ],
        });

        self.atlas_texture = Some(atlas);
        self.chunk_bind_group = Some(chunk_bg);

        // Create and store the chunk shader for lazy pipeline creation.
        // Pipeline is built via ensure_chunk_pipeline() so it uses the
        // correct surface format (not hardcoded Rgba8UnormSrgb).
        self.chunk_shader = Some(self.device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Chunk Shader"),
            source: wgpu::ShaderSource::Wgsl(CHUNK_SHADER_SRC.into()),
        }));
        self.ensure_chunk_pipeline();
    }

    /// Upload the MC lightmap texture for dynamic block/sky lighting.
    /// `pixels` is RGBA8 data (typically 16x16 = 256 texels for vanilla MC).
    /// Called from Java when the lightmap texture is updated (day/night cycle, torch placement, etc.).
    pub fn upload_lightmap(&mut self, pixels: &[u8], width: u32, height: u32) {
        if pixels.len() < (width * height * 4) as usize { return; }

        // Recreate texture if dimensions changed
        let need_new = self.lightmap_texture.as_ref().map_or(true, |t| {
            t.size().width != width || t.size().height != height
        });

        if need_new {
            log::info!("[dx12-wm] Creating lightmap texture: {}x{}", width, height);
            let tex = self.device.create_texture(&wgpu::TextureDescriptor {
                label: Some("Lightmap Texture"),
                size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                mip_level_count: 1,
                sample_count: 1,
                dimension: wgpu::TextureDimension::D2,
                format: wgpu::TextureFormat::Rgba8UnormSrgb,
                usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
                view_formats: &[],
            });
            self.lightmap_texture = Some(tex);
            self.lightmap_width = width;
            self.lightmap_height = height;

            // Rebuild the chunk bind group with the new lightmap texture
            if let (Some(atlas), Some(bgl)) = (&self.atlas_texture, &self.chunk_bind_group_layout) {
                let atlas_view = atlas.create_view(&wgpu::TextureViewDescriptor::default());
                let lightmap_view = self.lightmap_texture.as_ref()
                    .unwrap()
                    .create_view(&wgpu::TextureViewDescriptor::default());
                let bg = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
                    label: Some("Chunk Bind Group"),
                    layout: bgl,
                    entries: &[
                        wgpu::BindGroupEntry {
                            binding: 0,
                            resource: self.uniform_buffer.as_entire_binding(),
                        },
                        wgpu::BindGroupEntry {
                            binding: 1,
                            resource: wgpu::BindingResource::TextureView(&atlas_view),
                        },
                        wgpu::BindGroupEntry {
                            binding: 2,
                            resource: wgpu::BindingResource::Sampler(&self.atlas_sampler),
                        },
                        wgpu::BindGroupEntry {
                            binding: 3,
                            resource: wgpu::BindingResource::TextureView(&lightmap_view),
                        },
                        wgpu::BindGroupEntry {
                            binding: 4,
                            resource: wgpu::BindingResource::Sampler(&self.lightmap_sampler),
                        },
                    ],
                });
                self.chunk_bind_group = Some(bg);
            }

            eprintln!("[dx12-wm] Lightmap texture created: {}x{}", width, height);
        }

        // Upload pixel data to the (possibly new) lightmap texture
        if let Some(ref tex) = self.lightmap_texture {
            self.queue.write_texture(
                wgpu::ImageCopyTexture {
                    texture: tex,
                    mip_level: 0,
                    origin: wgpu::Origin3d::ZERO,
                    aspect: wgpu::TextureAspect::All,
                },
                &pixels[..(width * height * 4) as usize],
                wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(width * 4),
                    rows_per_image: Some(height),
                },
                wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
            );
        }

        log::info!("[dx12-wm] Lightmap uploaded: {}x{} ({} bytes)", width, height, pixels.len());
    }

    pub fn set_camera(&mut self, mvp: [[f32; 4]; 4]) {
        self.camera_target = mvp;
    }

    /// Set the camera world position (used to offset geometry near the camera).
    pub fn set_camera_pos(&mut self, x: f32, y: f32, z: f32) {
        self.camera_pos = [x, y, z];
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        if width == 0 || height == 0 {
            return;
        }
        self.width = width;
        self.height = height;

        // Surface mode: only update stored config dimensions.
        // Do NOT call surface.configure() or recreate depth here —
        // DXGI ResizeBuffers can throw a C++ exception when called while
        // GL is active on the same HWND.
        // The depth texture is recreated alongside the swapchain reconfig
        // in render_surface() when get_current_texture() returns Lost/Outdated.
        if let (Some(_), Some(ref mut config)) = (&self.surface, &mut self.surface_config) {
            config.width = width;
            config.height = height;
            self.resize_pending = true;
            log::info!("[dx12-wm] Surface resize pending to {}x{} (reconfig in render_surface)", width, height);
            return;
        }

        // Recreate offscreen slots
        for slot in self.slots.iter_mut() {
            *slot = Slot::new(&self.device, width, height);
        }
        self.idx = 0;
        self.pending_rx = [None, None, None];
    }

    /// Render one frame. Returns empty Vec in surface mode (D3D12 presents directly).
    pub fn render_frame(&mut self) -> Vec<u8> {
        if self.surface.is_some() {
            self.render_surface();
            return Vec::new();
        }
        self.render_offscreen()
    }

    /// Upload RGBA8 pixel data from GL framebuffer capture as a D3D12 texture.
    pub fn set_frame_pixels(&mut self, data: &[u8], width: u32, height: u32) {
        if width == 0 || height == 0 { return; }
        let size = (width * height * 4) as usize;

        // D3D12 requires bytes_per_row to be a multiple of 256.
        // glReadPixels returns tightly-packed rows, so we must repack.
        const ROW_ALIGN: u32 = 256;
        let src_row_bytes = width * 4;
        let dst_row_bytes = ((src_row_bytes + ROW_ALIGN - 1) / ROW_ALIGN) * ROW_ALIGN;
        let padded_size = (dst_row_bytes * height) as usize;

        // Recreate texture if dimensions changed
        let need_new = self.frame_texture.as_ref().map_or(true, |_t| {
            self.frame_width != width || self.frame_height != height
        });
        if need_new {
            log::info!("[dx12-wm] Creating new frame texture {}x{} (row {}→{} padded)",
                width, height, src_row_bytes, dst_row_bytes);
            let tex = self.device.create_texture(&wgpu::TextureDescriptor {
                label: Some("Frame Texture"),
                size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                mip_level_count: 1,
                sample_count: 1,
                dimension: wgpu::TextureDimension::D2,
                format: wgpu::TextureFormat::Rgba8UnormSrgb,
                usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
                view_formats: &[],
            });
            let view = tex.create_view(&wgpu::TextureViewDescriptor::default());
            let bind_group = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
                label: Some("Frame Bind Group"),
                layout: &self.tex_pipeline.get_bind_group_layout(0),
                entries: &[
                    wgpu::BindGroupEntry {
                        binding: 0,
                        resource: wgpu::BindingResource::TextureView(&view),
                    },
                    wgpu::BindGroupEntry {
                        binding: 1,
                        resource: wgpu::BindingResource::Sampler(&self.tex_sampler),
                    },
                ],
            });
            self.frame_texture = Some(tex);
            self.tex_bind_group = Some(bind_group);
            self.frame_width = width;
            self.frame_height = height;
            log::info!("[dx12-wm] Frame texture + bind_group created OK");
        }

        // Upload pixel data with D3D12-aligned row pitch (pad if needed)
        if let Some(ref tex) = self.frame_texture {
            let effective_len = data.len().min(size);
            if dst_row_bytes == src_row_bytes {
                // Tightly packed — direct upload
                self.queue.write_texture(
                    wgpu::ImageCopyTexture {
                        texture: tex,
                        mip_level: 0,
                        origin: wgpu::Origin3d::ZERO,
                        aspect: wgpu::TextureAspect::All,
                    },
                    &data[..effective_len],
                    wgpu::ImageDataLayout {
                        offset: 0,
                        bytes_per_row: Some(src_row_bytes),
                        rows_per_image: Some(height),
                    },
                    wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                );
            } else {
                // Repack: copy each row from tightly-packed source to padded destination
                let mut padded = vec![0u8; padded_size];
                let src = &data[..effective_len];
                for row in 0..height as usize {
                    let src_start = row * src_row_bytes as usize;
                    let dst_start = row * dst_row_bytes as usize;
                    let row_len = src_row_bytes as usize;
                    if src_start + row_len <= src.len() && dst_start + row_len <= padded.len() {
                        padded[dst_start..dst_start + row_len]
                            .copy_from_slice(&src[src_start..src_start + row_len]);
                    }
                }
                self.queue.write_texture(
                    wgpu::ImageCopyTexture {
                        texture: tex,
                        mip_level: 0,
                        origin: wgpu::Origin3d::ZERO,
                        aspect: wgpu::TextureAspect::All,
                    },
                    &padded,
                    wgpu::ImageDataLayout {
                        offset: 0,
                        bytes_per_row: Some(dst_row_bytes),
                        rows_per_image: Some(height),
                    },
                    wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                );
            }
        }
    }

    /// Store a chunk section mesh for D3D12 rendering.
    /// `data` contains MC vertex data (28 bytes/vertex for BLOCK format in MC 26.1.2).
    /// `section_x/y/z` are chunk section coordinates (world coord >> 4).
    pub fn upload_chunk_mesh(
        &mut self,
        section_x: i32,
        section_y: i32,
        section_z: i32,
        data: &[u8],
        vertex_count: u32,
        vertex_stride: u32,
    ) {
        let stride = vertex_stride as usize;
        let expected_size = (vertex_count as usize) * stride;
        if data.len() < expected_size || vertex_count == 0 || stride == 0 {
            log::warn!("[dx12-wm] Chunk mesh REJECTED: data.len={} < expected={}, vcount={}, stride={}",
                data.len(), expected_size, vertex_count, stride);
            return;
        }

        // MC uses GL_QUADS: 4 vertices per quad.
        // D3D12 uses TriangleList: need 6 indices per quad.
        let quad_count = vertex_count / 4;
        let tri_index_count = quad_count * 6;         // 6 indices per quad

        let world_ox = (section_x as f32) * 16.0;
        let world_oy = (section_y as f32) * 16.0;
        let world_oz = (section_z as f32) * 16.0;

        // Convert MC vertex data to ChunkVertex (position + color + uv + light_uv) and build index buffer.
        // MC 26.1.2 BLOCK format (28 bytes): Pos(12) + Color(4) + UV0(8) + UV2(4)
        // UV2 at offset 24: 2 u16 values (block_light, sky_light), range 0-240 (each light value * 16).
        let mut vertices: Vec<ChunkVertex> = Vec::with_capacity(vertex_count as usize);

        for v in 0..vertex_count {
            let base = (v as usize) * stride;
            // Need at least 28 bytes: Pos(12) + Color(4) + UV(8) + UV2(4)
            if base + 28 > data.len() { break; }

            // Position: 3 f32 at offset 0 (section-relative, 0..16)
            let px = f32::from_le_bytes([data[base], data[base+1], data[base+2], data[base+3]]);
            let py = f32::from_le_bytes([data[base+4], data[base+5], data[base+6], data[base+7]]);
            let pz = f32::from_le_bytes([data[base+8], data[base+9], data[base+10], data[base+11]]);

            // Color: 4 u8 (RGBA in memory) at offset 12 → float (used as lighting tint)
            let cr = data[base + 12] as f32 / 255.0;
            let cg = data[base + 13] as f32 / 255.0;
            let cb = data[base + 14] as f32 / 255.0;

            // UV: 2 f32 at offset 16 (texture atlas coords)
            let u = f32::from_le_bytes([data[base+16], data[base+17], data[base+18], data[base+19]]);
            let v_uv = f32::from_le_bytes([data[base+20], data[base+21], data[base+22], data[base+23]]);

            // Use raw MC vertex UVs directly — they are already correct for
            // the composited terrain atlas generated by the TextureAtlasMixin.
            let u_corrected = u.clamp(0.0, 1.0);
            let v_corrected = v_uv.clamp(0.0, 1.0);

            // UV2: 2 u16 at offset 24 (block_light, sky_light).
            // MC stores light levels as u16 (0, 16, 32, ..., 240).
            // Convert to lightmap UV coordinates: center of each 1/16 tile.
            let block_light = u16::from_le_bytes([data[base + 24], data[base + 25]]);
            let sky_light = u16::from_le_bytes([data[base + 26], data[base + 27]]);
            // Center of texel: (value + 0.5) / 16.0, equivalent to (value + 8) / 256.0
            let light_u = ((block_light as f32) + 8.0) / 256.0;
            let light_v = ((sky_light as f32) + 8.0) / 256.0;

            // World position (section origin + local pos).
            // Store directly in world space so the shader MVP transform is consistent
            // regardless of when the chunk was uploaded.
            let wx = px + world_ox;
            let wy = py + world_oy;
            let wz = pz + world_oz;

            vertices.push(ChunkVertex {
                position: [wx, wy, wz],
                color: [cr, cg, cb],
                uv: [u_corrected, v_corrected],
                light_uv: [light_u, light_v],
            });
        }

        // Diagnostic: dump first 4 vertices + atlas area on first chunk upload
        static mut FIRST_UPLOAD: bool = true;
        if unsafe { FIRST_UPLOAD } {
            unsafe { FIRST_UPLOAD = false; }
            eprintln!("[dx12-wm] First chunk upload: section=({},{},{}) stride={} vcount={} len={} camera=({:.1},{:.1},{:.1})",
                section_x, section_y, section_z, stride, vertex_count, data.len(),
                self.camera_pos[0], self.camera_pos[1], self.camera_pos[2]);
            // Dump raw bytes of first vertex to verify format
            if data.len() >= 28 {
                let raw = &data[0..28];
                eprintln!("[dx12-wm]   RAW v0 bytes: {:02X?}", raw);
                // Try reading UV at different offsets
                for off in [16usize, 20, 12, 8] {
                    if off + 8 <= data.len() {
                        let u = f32::from_le_bytes([data[off], data[off+1], data[off+2], data[off+3]]);
                        let v_val = f32::from_le_bytes([data[off+4], data[off+5], data[off+6], data[off+7]]);
                        eprintln!("[dx12-wm]     UV attempt at offset {}: ({:.6}, {:.6})", off, u, v_val);
                    }
                }
                // Check bytes at offset 24-27 (UV2/lightmap)
                if data.len() >= 28 {
                    let uv2_u = u16::from_le_bytes([data[24], data[25]]);
                    let uv2_v = u16::from_le_bytes([data[26], data[27]]);
                    eprintln!("[dx12-wm]     UV2 as u16 at offset 24: ({}, {})", uv2_u, uv2_v);
                }
                // Check if offset 24-27 are normal (bytes)
                eprintln!("[dx12-wm]     Normal at offset 24: ({}, {}, {})", data[24], data[25], data[26]);
            }
            // UV range statistics for ALL vertices in first chunk
            if !vertices.is_empty() {
                let mut u_min = f32::MAX; let mut u_max = f32::MIN;
                let mut v_min = f32::MAX; let mut v_max = f32::MIN;
                for vtx in &vertices {
                    u_min = u_min.min(vtx.uv[0]); u_max = u_max.max(vtx.uv[0]);
                    v_min = v_min.min(vtx.uv[1]); v_max = v_max.max(vtx.uv[1]);
                }
                eprintln!("[dx12-wm]   UV range over {} vertices: u=[{:.4},{:.4}] v=[{:.4},{:.4}]",
                    vertices.len(), u_min, u_max, v_min, v_max);
            }
            for i in 0..vertices.len().min(4) {
                let v = &vertices[i];
                eprintln!("[dx12-wm]   v[{}]: pos=({:.2},{:.2},{:.2}) color=({:.3},{:.3},{:.3}) uv=({:.4},{:.4})",
                    i, v.position[0], v.position[1], v.position[2],
                    v.color[0], v.color[1], v.color[2],
                    v.uv[0], v.uv[1]);
            }
            // Dump atlas pixel grid for all 4 corners of the first quad
            if let Some(ref pixels) = self.atlas_pixels {
                let aw = self.atlas_width as usize;
                let ah = self.atlas_height as usize;
                for vi in 0..vertices.len().min(4) {
                    let u = vertices[vi].uv[0].clamp(0.0, 1.0);
                    let v_uv = vertices[vi].uv[1].clamp(0.0, 1.0);
                    let px = (u * aw as f32) as usize;
                    let py = (v_uv * ah as f32) as usize;
                    let off = (py * aw + px) * 4;
                    if off + 4 <= pixels.len() {
                        eprintln!("[dx12-wm]   v[{}] atlas ({},{}) RGBA=({},{},{},{})",
                            vi, px, py,
                            pixels[off], pixels[off+1], pixels[off+2], pixels[off+3]);
                    } else {
                        eprintln!("[dx12-wm]   v[{}] atlas ({},{}) OUT OF BOUNDS", vi, px, py);
                    }
                }
                // Dump a 4x4 grid of pixels inside the first quad (16x16 atlas area)
                // Show 5 sample pixels per row: start, 25%, 50%, 75%, end
                let aw_f = self.atlas_width as f32;
                let ah_f = self.atlas_height as f32;
                let u0 = vertices[0].uv[0].clamp(0.0, 1.0);
                let u1 = vertices[1].uv[0].clamp(0.0, 1.0);
                let v0 = vertices[0].uv[1].clamp(0.0, 1.0);
                let v2 = vertices[2].uv[1].clamp(0.0, 1.0);
                eprintln!("[dx12-wm]   16x16 atlas quad uv_x=[{:.4},{:.4}] uv_y=[{:.4},{:.4}]",
                    u0.min(u1), u0.max(u1), v0.min(v2), v0.max(v2));
                for row_pct in [0.0, 0.25, 0.5, 0.75, 1.0] {
                    let row = v0 + (v2 - v0) * row_pct as f32;
                    let py = (row * ah_f) as usize;
                    let mut line = format!("[dx12-wm]   row y={:.1}% (pixel y={}):", row_pct * 100.0, py);
                    for col_pct in [0.0, 0.25, 0.5, 0.75, 1.0] {
                        let col = u0 + (u1 - u0) * col_pct as f32;
                        let px = (col * aw_f) as usize;
                        let off = (py * aw + px) * 4;
                        if off + 4 <= pixels.len() {
                            let r = pixels[off]; let g = pixels[off+1];
                            let b = pixels[off+2]; let a = pixels[off+3];
                            line.push_str(&format!(" ({},{})→({},{},{},{})", px, py, r, g, b, a));
                        }
                    }
                    eprintln!("{}", line);
                }
                // Scan atlas for nearest non-zero pixel around the ACTUAL chunk UV position
                let scan_radius = 500i32; // scan up to 500px in each direction
                // Use first vertex's UV to determine scan center
                let u_center = vertices[0].uv[0].clamp(0.0, 1.0);
                let v_center = vertices[0].uv[1].clamp(0.0, 1.0);
                let cx = (u_center * aw as f32) as i32;
                let cy = (v_center * ah as f32) as i32;
                eprintln!("[dx12-wm]   SCAN_CENTER from v[0] uv=({:.4},{:.4}) → pixel=({},{})", u_center, v_center, cx, cy);
                // First: check if the exact chunk UV area has any non-zero pixel
                // Sample up to 5x5 grid around scan center
                let mut chunk_has_data = false;
                for dy in -2i32..=2 {
                    for dx in -2i32..=2 {
                        let px = cx + dx;
                        let py = cy + dy;
                        if px < 0 || px >= aw as i32 || py < 0 || py >= ah as i32 { continue; }
                        let off = ((py as usize) * aw + (px as usize)) * 4;
                        if off + 4 <= pixels.len() {
                            if pixels[off+3] != 0 {
                                if !chunk_has_data {
                                    eprintln!("[dx12-wm]   CHUNK_AREA_HAS_DATA first at ({},{}): RGBA=({},{},{},{})",
                                        px, py, pixels[off], pixels[off+1], pixels[off+2], pixels[off+3]);
                                    chunk_has_data = true;
                                }
                            }
                        }
                    }
                }
                if !chunk_has_data {
                    eprintln!("[dx12-wm]   CHUNK_AREA_EMPTY: no non-zero pixel within 5x5 of ({},{})", cx, cy);
                }
                // Spiral search: find nearest non-zero pixel anywhere in atlas
                let mut found_nonzero = false;
                for r in 1..=scan_radius {
                    // Search perimeter of square with radius r centered on chunk UV
                    for dy in -r..=r {
                        for dx in -r..=r {
                            if dx.abs() != r && dy.abs() != r { continue; }
                            let px = cx + dx;
                            let py = cy + dy;
                            if px < 0 || px >= aw as i32 || py < 0 || py >= ah as i32 { continue; }
                            let off = ((py as usize) * aw + (px as usize)) * 4;
                            if off + 4 <= pixels.len() {
                                if pixels[off+3] != 0 {
                                    eprintln!("[dx12-wm]   NEAREST_NONZERO at dx={} dy={} pixel=({},{}) dist={}: RGBA=({},{},{},{})",
                                        dx, dy, px, py, r,
                                        pixels[off], pixels[off+1], pixels[off+2], pixels[off+3]);
                                    found_nonzero = true;
                                    break;
                                }
                            }
                        }
                        if found_nonzero { break; }
                    }
                    if found_nonzero { break; }
                }
                if !found_nonzero {
                    eprintln!("[dx12-wm]   NO_NONZERO_PIXEL within {}px of chunk UV area", scan_radius);
                }
            }
        }

        if vertices.is_empty() { return; }

        // Build triangle indices (GL_QUADS → 2 triangles). Always u32 so the
        // merged index buffer keeps a single format (Phase 11e batching).
        let mut indices: Vec<u32> = Vec::with_capacity(tri_index_count as usize);
        for q in 0..quad_count {
            let vi = q * 4;
            if vi + 3 >= vertex_count { break; }
            indices.extend_from_slice(&[vi, vi + 1, vi + 2, vi, vi + 2, vi + 3]);
        }
        if indices.is_empty() { return; }

        let vcount = vertices.len() as u32;
        let icount = indices.len() as u32;

        // Phase 11j: assign merge offsets NOW (append position in the flat
        // caches), not during a later full rebuild. This lets uploads append
        // incrementally without re-concatenating every mesh every frame.
        let base_vertex = self.merged_verts.len() as u32;
        let index_offset = self.merged_indices.len() as u32;

        // wgpu 23 D3D12 hard limit (268435456 bytes) pre-check BEFORE this
        // mesh is recorded anywhere — appending to merged_verts/indices and
        // then failing the GPU buffer create would leave the CPU caches
        // inconsistent with the GPU buffers (draws would read garbage).
        let new_vb_bytes = (base_vertex as u64 + vcount as u64)
            * std::mem::size_of::<ChunkVertex>() as u64;
        let new_ib_bytes = (index_offset as u64 + icount as u64)
            * std::mem::size_of::<u32>() as u64;
        if new_vb_bytes > Self::MAX_BUF_SIZE || new_ib_bytes > Self::MAX_BUF_SIZE {
            log::error!("[dx12-wm] Chunk mesh REJECTED (section {},{},{}): merged VB {} B / IB {} B > wgpu max {} B",
                section_x, section_y, section_z, new_vb_bytes, new_ib_bytes, Self::MAX_BUF_SIZE);
            return;
        }

        let mesh = ChunkMesh {
            vertices,
            indices,
            base_vertex,   // assigned at upload time (Phase 11j incremental merge)
            index_offset,
            index_count: icount,
        };

        let key = (section_x, section_y, section_z);
        self.chunk_meshes.entry(key).or_insert_with(Vec::new).push(mesh);
        self.has_chunk_geometry = true;

        // Phase 11j: append-only merge. Extend the CPU flat caches and upload
        // just the new mesh slice to the GPU, growing the GPU buffers on
        // demand. No full rebuild — previously every upload flagged
        // chunk_geometry_dirty, so prepare_chunk_batch() re-concatenated ALL
        // meshes and recreated both GPU buffers every frame while loading
        // (log evidence: 4000+ meshes / 5.2M verts rebuilt repeatedly → 6 FPS).
        let verts_start = base_vertex as usize;
        let idxs_start = index_offset as usize;
        self.merged_verts.extend_from_slice(&self.chunk_meshes[&key].last().unwrap().vertices);
        self.merged_indices.extend_from_slice(&self.chunk_meshes[&key].last().unwrap().indices);
        self.upload_chunk_slice(verts_start, idxs_start);

        log::info!("[dx12-wm] Chunk mesh uploaded: section=({},{},{}) {} verts, {} indices (incremental)",
            section_x, section_y, section_z, vcount, icount);
    }

    /// Set fog color and density for atmospheric fog effect.
    /// fog_color_rgb = fog color (e.g. sky color from MC)
    /// fog_density   = how quickly fog builds up with distance
    pub fn set_fog(&mut self, fog_color_rgb: &[f32; 3], fog_density: f32) {
        self.fog_color = [fog_color_rgb[0], fog_color_rgb[1], fog_color_rgb[2], fog_density];
    }

    /// Set sky dome zenith color (used for sky gradient top color).
    /// Typically from MC's sky color, brightened for the top of the dome.
    pub fn set_sky_color(&mut self, rgb: &[f32; 3]) {
        self.sky_color = [rgb[0], rgb[1], rgb[2]];
    }

    /// Remove all chunk meshes for a given section.
    /// Called before recompiling a section to prevent stale mesh accumulation.
    pub fn clear_chunk_section(&mut self, section_x: i32, section_y: i32, section_z: i32) {
        let key = (section_x, section_y, section_z);
        if self.chunk_meshes.remove(&key).is_some() {
            // Phase 11j: removals shift every later mesh's base_vertex /
            // index_offset, so they must trigger a full rebuild. This is far
            // rarer than uploads (only on section recompile/unload), so the
            // cost is acceptable — unlike the old per-upload full rebuild.
            self.chunk_need_full_rebuild = true;
        }
    }

    /// Phase 11j: append the mesh slice at `verts_start`/`idxs_start` in the
    /// flat caches into the merged GPU buffers. Grows `chunk_vb`/`chunk_ib`
    /// when the append exceeds their current allocation (copying existing data
    /// into the bigger buffer), otherwise issues a plain `queue.write_buffer`
    /// for just the new range.
    ///
    /// Growth uses async `queue.write_buffer` (NOT `mapped_at_creation` +
    /// `get_mapped_range_mut` + `unmap`): the synchronous mapped path copies
    /// the whole multi-hundred-MB buffer on the calling thread, which under a
    /// heavy chunk-compile burst (all Worker-Main threads uploading) stalled
    /// the GPU long enough for a TDR → DeviceLost → wgpu fatal panic → JVM
    /// crash (hs_err_pid23980, wgpu create_buffer → handle_error_inner →
    /// panic_unwind → EXCEPTION_UNCAUGHT_CXX_EXCEPTION).
    ///
    /// wgpu 23 D3D12 hard limit: max buffer size 268435456 (256 MB).
    /// create_buffer with a larger size is a Validation Error that panics
    /// inside wgpu (game log: "Buffer size 305925120 is greater than the
    /// maximum buffer size (268435456)" — hit when the merged VB ×2 growth
    /// jumped 146 MB → 292 MB). Growth is clamped to this limit and uploads
    /// that alone exceed it are rejected BEFORE appending so the CPU caches
    /// stay consistent with the GPU buffers.
    const CHUNK_VB_MIN_CAP: u64 = 4 * 1024 * 1024; // 4 MB
    const CHUNK_IB_MIN_CAP: u64 = 1 * 1024 * 1024; // 1 MB
    const MAX_BUF_SIZE: u64 = 256 * 1024 * 1024;   // wgpu D3D12 hard limit (268435456)

    fn upload_chunk_slice(&mut self, verts_start: usize, idxs_start: usize) {
        if self.merged_verts.len() <= verts_start || self.merged_indices.len() <= idxs_start {
            return;
        }
        let verts = &self.merged_verts[verts_start..];
        let idxs = &self.merged_indices[idxs_start..];

        let vb_bytes = (verts_start as u64 + verts.len() as u64)
            * std::mem::size_of::<ChunkVertex>() as u64;
        let ib_bytes = (idxs_start as u64 + idxs.len() as u64)
            * std::mem::size_of::<u32>() as u64;

        // Vertex buffer: grow (async upload of ALL data) or append (write_buffer).
        let vb_grows = self.chunk_vb.as_ref().map_or(true, |b| b.size() < vb_bytes);
        if vb_grows {
            if vb_bytes > Self::MAX_BUF_SIZE {
                // Data alone exceeds the 256 MB hard limit; can never fit.
                // (upload_chunk_mesh pre-checks too — this is belt & braces.)
                log::error!("[dx12-wm] Chunk VB needs {} bytes > wgpu max {}; upload skipped",
                    vb_bytes, Self::MAX_BUF_SIZE);
                return;
            }
            let mut new_cap = (self.chunk_vb_capacity * 2)
                .max(vb_bytes)
                .max(Self::CHUNK_VB_MIN_CAP);
            if new_cap > Self::MAX_BUF_SIZE {
                new_cap = Self::MAX_BUF_SIZE;
            }
            let vb = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Chunk Merged VB"),
                size: new_cap,
                usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
                mapped_at_creation: false,
            });
            // Resize path: upload the ENTIRE merged cache via async
            // queue.write_buffer (driver-managed staging), not a synchronous
            // mapped copy + unmap.
            self.queue.write_buffer(&vb, 0, bytemuck::cast_slice(&self.merged_verts));
            self.chunk_vb = Some(vb);
            self.chunk_vb_capacity = new_cap;
            log::info!("[dx12-wm] Chunk VB grown: {} MB ({} verts)",
                new_cap / (1024 * 1024), self.merged_verts.len());
        } else if let Some(vb) = &self.chunk_vb {
            let start_bytes = (verts_start as u64) * std::mem::size_of::<ChunkVertex>() as u64;
            self.queue.write_buffer(vb, start_bytes, bytemuck::cast_slice(verts));
        }

        // Index buffer: grow (async upload of ALL data) or append (write_buffer).
        let ib_grows = self.chunk_ib.as_ref().map_or(true, |b| b.size() < ib_bytes);
        if ib_grows {
            if ib_bytes > Self::MAX_BUF_SIZE {
                log::error!("[dx12-wm] Chunk IB needs {} bytes > wgpu max {}; upload skipped",
                    ib_bytes, Self::MAX_BUF_SIZE);
                return;
            }
            let mut new_cap = (self.chunk_ib_capacity * 2)
                .max(ib_bytes)
                .max(Self::CHUNK_IB_MIN_CAP);
            if new_cap > Self::MAX_BUF_SIZE {
                new_cap = Self::MAX_BUF_SIZE;
            }
            let ib = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Chunk Merged IB"),
                size: new_cap,
                usage: wgpu::BufferUsages::INDEX | wgpu::BufferUsages::COPY_DST,
                mapped_at_creation: false,
            });
            self.queue.write_buffer(&ib, 0, bytemuck::cast_slice(&self.merged_indices));
            self.chunk_ib = Some(ib);
            self.chunk_ib_capacity = new_cap;
            log::info!("[dx12-wm] Chunk IB grown: {} MB ({} indices)",
                new_cap / (1024 * 1024), self.merged_indices.len());
        } else if let Some(ib) = &self.chunk_ib {
            let start_bytes = (idxs_start as u64) * std::mem::size_of::<u32>() as u64;
            self.queue.write_buffer(ib, start_bytes, bytemuck::cast_slice(idxs));
        }

        // Indirect args buffer holds the *visible* draw list, rewritten every
        // frame in draw_chunks() — it only needs growing when the total mesh
        // count (an upper bound on visible draws) outgrows the current
        // capacity. next_power_of_two keeps the growth logarithmic.
        let total_meshes: u32 = self.chunk_meshes.values().map(|v| v.len() as u32).sum();
        if self.chunk_indirect.as_ref().map_or(true, |_| {
            total_meshes > self.chunk_indirect_capacity
        }) {
            let capacity = total_meshes.max(1024).next_power_of_two();
            let indirect = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Chunk Indirect Args"),
                size: (std::mem::size_of::<DrawIndexedIndirectArgs>() * capacity as usize)
                    as wgpu::BufferAddress,
                usage: wgpu::BufferUsages::INDIRECT | wgpu::BufferUsages::COPY_DST,
                mapped_at_creation: false,
            });
            self.chunk_indirect = Some(indirect);
            self.chunk_indirect_capacity = capacity;
        }
    }

    // ── Draw calls shared by surface and offscreen modes ──────────

    fn draw_scene<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        rp.set_pipeline(&self.pipeline);
        rp.set_bind_group(0, &self.bind_group, &[]);

        // Plane (identity model baked into vertices)
        rp.set_vertex_buffer(0, self.plane_vb.slice(..));
        rp.set_index_buffer(self.plane_ib.slice(..), wgpu::IndexFormat::Uint16);
        rp.draw_indexed(0..self.plane_count, 0, 0..1);

        // Cubes (each VB has pre-offsetted vertices)
        for cube_vb in &self.cube_vbs {
            rp.set_vertex_buffer(0, cube_vb.slice(..));
            rp.set_index_buffer(self.cube_ib.slice(..), wgpu::IndexFormat::Uint16);
            rp.draw_indexed(0..self.cube_count, 0, 0..1);
        }
    }

    /// Phase 11e: rebuild the merged vertex/index/indirect buffers from the
    /// CPU-side mesh storage. Phase 11j: called only when a section was
    /// cleared (removals shift offsets) — plain uploads now append
    /// incrementally in `upload_chunk_slice` and skip this path entirely.
    fn rebuild_chunk_buffers(&mut self) {
        self.chunk_geometry_dirty = false;
        self.chunk_need_full_rebuild = false;

        // Rebuild the flat CPU caches from scratch (a clear/removal happened),
        // then re-upload everything into (possibly larger) GPU buffers.
        let mut all_verts: Vec<ChunkVertex> = Vec::new();
        let mut all_indices: Vec<u32> = Vec::new();
        let mut mesh_count: u32 = 0;
        for meshes in self.chunk_meshes.values_mut() {
            for m in meshes.iter_mut() {
                m.base_vertex = all_verts.len() as u32;
                m.index_offset = all_indices.len() as u32;
                m.index_count = m.indices.len() as u32;
                all_verts.extend_from_slice(&m.vertices);
                all_indices.extend_from_slice(&m.indices);
                mesh_count += 1;
            }
        }
        self.merged_verts = all_verts;
        self.merged_indices = all_indices;

        if self.merged_verts.is_empty() {
            self.chunk_vb = None;
            self.chunk_ib = None;
            self.chunk_indirect = None;
            self.chunk_indirect_capacity = 0;
            self.chunk_vb_capacity = 0;
            self.chunk_ib_capacity = 0;
            self.has_chunk_geometry = false;
            return;
        }

        let vb_cap = ((std::mem::size_of::<ChunkVertex>() * self.merged_verts.len())
            .max(Self::CHUNK_VB_MIN_CAP as usize))
            .min(Self::MAX_BUF_SIZE as usize) as wgpu::BufferAddress;
        let vb = self.device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Chunk Merged VB"),
            size: vb_cap,
            usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });
        self.queue.write_buffer(&vb, 0, bytemuck::cast_slice(&self.merged_verts));
        self.chunk_vb = Some(vb);
        self.chunk_vb_capacity = vb_cap;

        let ib_cap = ((std::mem::size_of::<u32>() * self.merged_indices.len())
            .max(Self::CHUNK_IB_MIN_CAP as usize))
            .min(Self::MAX_BUF_SIZE as usize) as wgpu::BufferAddress;
        let ib = self.device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Chunk Merged IB"),
            size: ib_cap,
            usage: wgpu::BufferUsages::INDEX | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });
        self.queue.write_buffer(&ib, 0, bytemuck::cast_slice(&self.merged_indices));
        self.chunk_ib = Some(ib);
        self.chunk_ib_capacity = ib_cap;

        // Indirect args buffer: one 16-byte entry per mesh. Needs INDIRECT
        // for multi_draw_indexed_indirect and COPY_DST for queue.write_buffer.
        let capacity = mesh_count.max(1024).next_power_of_two();
        let indirect = self.device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Chunk Indirect Args"),
            size: (std::mem::size_of::<DrawIndexedIndirectArgs>() * capacity as usize)
                as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::INDIRECT | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        self.chunk_indirect = Some(indirect);
        self.chunk_indirect_capacity = capacity;
        self.has_chunk_geometry = true;

        eprintln!("[dx12-wm] Chunk batch rebuilt: {} meshes, {} verts, {} indices (indirect capacity {})",
            mesh_count, self.merged_verts.len(), self.merged_indices.len(), capacity);
    }

    /// Ensure the merged chunk buffers are up to date before issuing draws.
    /// Phase 11j: uploads are applied incrementally at upload time, so this
    /// only performs a (rare) full rebuild after a section clear/removal.
    fn prepare_chunk_batch(&mut self) {
        if self.chunk_need_full_rebuild || self.chunk_geometry_dirty {
            self.rebuild_chunk_buffers();
        }
    }

    /// Render all stored MC chunk meshes using two-pass rendering.
    ///
    /// Pass 1 (Opaque): depth_write=true, blend=None
    ///   — Handles SOLID and CUTOUT blocks (including leaves, plants).
    ///     Shader discards tex_color.a < 0.05 (fully transparent pixels are gaps).
    ///     Opaque pixels write depth so they correctly occlude geometry behind them.
    ///
    /// Pass 2 (Transparent): depth_write=false, blend=ALPHA_BLENDING
    ///   — Handles TRANSLUCENT blocks (water, stained glass, etc.).
    ///     Shader discards both fully transparent (a<0.05) AND fully opaque (a>0.95)
    ///     pixels — only semi-transparent fragments pass through for alpha blending.
    ///     Depth write is DISABLED so these fragments never occlude objects behind them.
    ///
    /// Phase 11e: both passes are issued as a single multi_draw_indexed_indirect
    /// over the AABB-culled draw list, instead of per-mesh state changes.
    fn draw_chunks<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        let Some(pipeline) = &self.chunk_pipeline else { return; };
        let Some(pipeline_transparent) = &self.chunk_pipeline_transparent else { return; };
        let Some(bind_group) = &self.chunk_bind_group else { return; };
        let Some(vb) = &self.chunk_vb else { return; };
        let Some(ib) = &self.chunk_ib else { return; };
        let Some(indirect) = &self.chunk_indirect else { return; };

        // Phase 11f: frustum culling — skip chunk sections outside the view.
        // Section AABBs are in world space; camera_mvp maps world → clip
        // (JOML perspective with zZeroToOne=true, D3D-style depth).
        // A real projection matrix has row 4 = [0, 0, 1, 0]; the initial
        // IDENTITY value has z=0 there, so culling is skipped until the
        // first camera update arrives.
        let cull_enabled = self.camera_mvp[3][2] != 0.0;
        let planes = if cull_enabled {
            // camera_mvp lives in column-major memory (JOML Matrix4f.get());
            // the WGSL uniform consumes that layout as-is, but plane extraction
            // expects row-major — transpose first. (Fix: looking up culled every
            // section because planes were extracted from the transposed matrix.)
            let mvp_row_major = transpose4(&self.camera_mvp);
            extract_frustum_planes(&mvp_row_major)
        } else {
            [[0.0f32; 4]; 6] // all-zero planes pass every box
        };

        static mut CHUNK_DRAW_FIRST: bool = true;
        static mut CHUNK_FRAME_COUNT: u32 = 0;
        static mut CHUNK_CULLED_TOTAL: u32 = 0;
        static mut CHUNK_VISIBLE_TOTAL: u32 = 0;
        if unsafe { CHUNK_DRAW_FIRST } {
            unsafe { CHUNK_DRAW_FIRST = false; }
            let total_meshes: usize = self.chunk_meshes.values().map(|v| v.len()).sum();
            eprintln!("[dx12-wm] draw_chunks: {} sections, {} meshes total, batching={}",
                self.chunk_meshes.len(), total_meshes, self.chunk_batch_enabled);
        }

        // Phase 11e: one AABB-culled draw list shared by both passes.
        let (draws, visible_sections, culled_sections) =
            collect_visible_draws(self.chunk_meshes.iter(), &planes);

        if !draws.is_empty() && (draws.len() as u32) <= self.chunk_indirect_capacity {
            // Per-frame upload of the visible draw list (a few KB). The merged
            // VB/IB are static; only the args change with the camera.
            self.queue.write_buffer(indirect, 0, bytemuck::cast_slice(&draws));

            // ════════════════════════════════════════════════
            // Pass 1: Opaque — writes depth, no blending
            // ════════════════════════════════════════════════
            rp.set_pipeline(pipeline);
            rp.set_bind_group(0, bind_group, &[]);
            rp.set_vertex_buffer(0, vb.slice(..));
            rp.set_index_buffer(ib.slice(..), wgpu::IndexFormat::Uint32);
            if self.chunk_batch_enabled {
                rp.multi_draw_indexed_indirect(indirect, 0, draws.len() as u32);
            } else {
                for d in &draws {
                    rp.draw_indexed(d.first_index..d.first_index + d.index_count, d.base_vertex, 0..1);
                }
            }

            // ════════════════════════════════════════════════
            // Pass 2: Transparent — no depth write, alpha blending
            // ════════════════════════════════════════════════
            rp.set_pipeline(pipeline_transparent);
            rp.set_bind_group(0, bind_group, &[]);
            if self.chunk_batch_enabled {
                rp.multi_draw_indexed_indirect(indirect, 0, draws.len() as u32);
            } else {
                for d in &draws {
                    rp.draw_indexed(d.first_index..d.first_index + d.index_count, d.base_vertex, 0..1);
                }
            }
        }

        // Throttled culling stats (every 600 frames ≈ 10 s)
        unsafe {
            CHUNK_CULLED_TOTAL += culled_sections;
            CHUNK_VISIBLE_TOTAL += visible_sections;
            CHUNK_FRAME_COUNT += 1;
            if CHUNK_FRAME_COUNT % 600 == 0 {
                let visible = CHUNK_VISIBLE_TOTAL;
                let culled = CHUNK_CULLED_TOTAL;
                eprintln!("[dx12-wm] frustum: {} visible / {} culled (sections this frame)",
                    visible, culled);
                CHUNK_VISIBLE_TOTAL = 0;
                CHUNK_CULLED_TOTAL = 0;
            }
        }
    }

    /// Pure builder: 9 floats/entity → 36 non-indexed box vertices
    /// (6 faces × 2 triangles × 3 verts). Returns None when the entity count
    /// is 0 or exceeds 256 (invalid upload).
    fn build_entity_vertices(data: &[f32]) -> Option<Vec<Vertex>> {
        const PER_ENTITY: usize = 9;
        let entity_count = data.len() / PER_ENTITY;
        if entity_count == 0 || entity_count > 256 {
            return None;
        }

        // Generate 36 vertices per entity (6 faces × 2 triangles × 3 verts, non-indexed)
        let mut vertices: Vec<Vertex> = Vec::with_capacity(entity_count * 36);

        for i in 0..entity_count {
            let off = i * PER_ENTITY;
            let cx = data[off];
            let cy = data[off + 1];
            let cz = data[off + 2];
            let sx = (data[off + 3] * 0.5).max(0.01);
            let sy = (data[off + 4] * 0.5).max(0.01);
            let sz = (data[off + 5] * 0.5).max(0.01);
            let r = data[off + 6].clamp(0.0, 1.0);
            let g = data[off + 7].clamp(0.0, 1.0);
            let b = data[off + 8].clamp(0.0, 1.0);
            let col = [r, g, b];

            let (min_x, max_x) = (cx - sx, cx + sx);
            let (min_y, max_y) = (cy - sy, cy + sy);
            let (min_z, max_z) = (cz - sz, cz + sz);

            // Helper: push 3 verts for a triangle
            let mut tri = |a: [f32; 3], b: [f32; 3], c: [f32; 3]| {
                vertices.push(Vertex { position: a, color: col });
                vertices.push(Vertex { position: b, color: col });
                vertices.push(Vertex { position: c, color: col });
            };
            // Front (+z)
            tri([min_x, min_y, max_z], [max_x, max_y, max_z], [max_x, min_y, max_z]);
            tri([min_x, min_y, max_z], [min_x, max_y, max_z], [max_x, max_y, max_z]);
            // Back (-z)
            tri([max_x, min_y, min_z], [max_x, max_y, min_z], [min_x, min_y, min_z]);
            tri([min_x, min_y, min_z], [max_x, max_y, min_z], [min_x, max_y, min_z]);
            // Top (+y)
            tri([min_x, max_y, min_z], [max_x, max_y, max_z], [min_x, max_y, max_z]);
            tri([min_x, max_y, min_z], [max_x, max_y, min_z], [max_x, max_y, max_z]);
            // Bottom (-y)
            tri([min_x, min_y, max_z], [max_x, min_y, min_z], [min_x, min_y, min_z]);
            tri([min_x, min_y, max_z], [max_x, min_y, max_z], [max_x, min_y, min_z]);
            // Right (+x)
            tri([max_x, min_y, min_z], [max_x, max_y, max_z], [max_x, min_y, max_z]);
            tri([max_x, min_y, min_z], [max_x, max_y, min_z], [max_x, max_y, max_z]);
            // Left (-x)
            tri([min_x, min_y, max_z], [min_x, max_y, min_z], [min_x, min_y, min_z]);
            tri([min_x, min_y, max_z], [min_x, max_y, max_z], [min_x, max_y, min_z]);
        }
        Some(vertices)
    }

    /// Upload entity data and rebuild the entity vertex buffer.
    /// data format: [x, y, z, w, h, d, r, g, b] per entity (9 floats).
    /// Each entity is rendered as a colored box matching its bounding box dimensions.
    pub fn set_entities(&mut self, data: &[f32]) {
        // Phase 11g: skip redundant uploads when the data is unchanged
        if self.last_entity_data == data {
            return;
        }
        self.last_entity_data = data.to_vec();
        self.entity_uploads += 1;

        let Some(vertices) = Self::build_entity_vertices(data) else {
            // 0 entities → clear; >256 is unreasonable, likely corrupted data
            self.entity_count = 0;
            return;
        };
        let total_vertices = vertices.len() as u32;

        // Recreate vertex buffer if needed
        let vb_size = (total_vertices as usize * size_of::<Vertex>()) as u64;
        if self.entity_buffer.as_ref().map_or(true, |b| b.size() < vb_size) {
            self.entity_buffer = Some(self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Entity VB"),
                size: vb_size.max(1),
                usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
                mapped_at_creation: false,
            }));
        }
        self.queue.write_buffer(self.entity_buffer.as_ref().unwrap(), 0, bytemuck::cast_slice(&vertices));
        self.entity_count = total_vertices;

        // Ensure entity pipeline exists
        self.ensure_entity_pipeline();
    }

    /// Render entity boxes in the current render pass.
    fn draw_entities<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        if self.entity_count == 0 { return; }
        let Some(pipeline) = &self.entity_pipeline else { return; };
        let Some(vb) = &self.entity_buffer else { return; };
        let Some(bind_group) = &self.chunk_bind_group else { return; };

        rp.set_pipeline(pipeline);
        rp.set_bind_group(0, bind_group, &[]);
        rp.set_vertex_buffer(0, vb.slice(..));
        rp.draw(0..self.entity_count, 0..1);
    }

    /// Pure builder: 8 floats/particle → 6 billboard vertices each (two
    /// triangles forming a screen-space quad).
    /// Returns None when the particle count is 0 or exceeds 2048 (invalid upload).
    fn build_particle_vertices(data: &[f32]) -> Option<Vec<ParticleVertex>> {
        const PER_PARTICLE: usize = 8;
        let particle_count = data.len() / PER_PARTICLE;
        if particle_count == 0 || particle_count > 2048 {
            return None;
        }

        // 6 vertices per particle: (-,-) (+,-) (+,+) (-,-) (+,+) (-,+)
        const QUAD: [[f32; 2]; 6] = [
            [-0.5, -0.5],
            [0.5, -0.5],
            [0.5, 0.5],
            [-0.5, -0.5],
            [0.5, 0.5],
            [-0.5, 0.5],
        ];

        let mut vertices: Vec<ParticleVertex> = Vec::with_capacity(particle_count * 6);
        for i in 0..particle_count {
            let off = i * PER_PARTICLE;
            let center = [data[off], data[off + 1], data[off + 2]];
            let color = [
                data[off + 4].clamp(0.0, 1.0),
                data[off + 5].clamp(0.0, 1.0),
                data[off + 6].clamp(0.0, 1.0),
                data[off + 7].clamp(0.0, 1.0),
            ];
            let size = data[off + 3].max(1.0);
            for corner in QUAD {
                vertices.push(ParticleVertex {
                    position: center,
                    color,
                    size,
                    corner,
                });
            }
        }
        Some(vertices)
    }

    /// Upload particle data and rebuild the particle point buffer.
    /// data format: [x, y, z, size, r, g, b, a] per particle (8 floats).
    pub fn set_particles(&mut self, data: &[f32]) {
        // Phase 11g: skip redundant uploads when the data is unchanged
        if self.last_particle_data == data {
            return;
        }
        self.last_particle_data = data.to_vec();
        self.particle_uploads += 1;

        let Some(particles) = Self::build_particle_vertices(data) else {
            self.particle_count = 0;
            return;
        };
        let particle_count = particles.len() as u32;

        let vb_size = (particle_count as usize * size_of::<ParticleVertex>()) as u64;
        if self.particle_buffer.as_ref().map_or(true, |b| b.size() < vb_size) {
            self.particle_buffer = Some(self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Particle VB"),
                size: vb_size.max(1),
                usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
                mapped_at_creation: false,
            }));
        }
        self.queue.write_buffer(self.particle_buffer.as_ref().unwrap(), 0, bytemuck::cast_slice(&particles));
        self.particle_count = particle_count;

        // Ensure particle pipeline exists
        self.ensure_particle_pipeline();
    }

    /// Render particle point sprites in the current render pass.
    fn draw_particles<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        if self.particle_count == 0 { return; }
        let Some(pipeline) = &self.particle_pipeline else { return; };
        let Some(vb) = &self.particle_buffer else { return; };
        let Some(bind_group) = &self.chunk_bind_group else { return; };
        rp.set_pipeline(pipeline);
        rp.set_bind_group(0, bind_group, &[]);
        rp.set_vertex_buffer(0, vb.slice(..));
        rp.draw(0..self.particle_count, 0..1);
    }

    /// Render the sky dome (gradient hemisphere) in the current render pass.
    fn draw_sky_dome<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        let Some(pipeline) = &self.sky_pipeline else { return; };
        let Some(vb) = &self.sky_vb else { return; };
        let Some(ib) = &self.sky_ib else { return; };
        let Some(bind_group) = &self.chunk_bind_group else { return; };
        if self.sky_index_count == 0 { return; }
        rp.set_pipeline(pipeline);
        rp.set_bind_group(0, bind_group, &[]);
        rp.set_vertex_buffer(0, vb.slice(..));
        rp.set_index_buffer(ib.slice(..), wgpu::IndexFormat::Uint16);
        rp.draw_indexed(0..self.sky_index_count, 0, 0..1);
    }

    /// Create or recreate the chunk render pipelines using the current surface format.
    /// Two pipelines are created:
    ///   1. Opaque pipeline: depth_write=true, blend=None — handles SOLID and CUTOUT blocks
    ///   2. Transparent pipeline: depth_write=false, blend=ALPHA_BLENDING — handles TRANSLUCENT blocks
    /// Called after atlas upload and after surface initialization to ensure
    /// the color target format matches the swapchain.
    fn ensure_chunk_pipeline(&mut self) {
        if self.chunk_pipeline.is_some() { return; }

        let Some(shader) = &self.chunk_shader else { return; };
        let Some(bgl) = &self.chunk_bind_group_layout else { return; };

        let pipeline_layout = self.device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Chunk PL"),
            bind_group_layouts: &[bgl],
            push_constant_ranges: &[],
        });

        // ---- Opaque pipeline: depth_write ON, no blending ----
        let pipeline = self.device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Chunk Pipeline (Opaque)"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: shader,
                entry_point: Some("vs_main"),
                buffers: &[ChunkVertex::desc()],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: self.surface_format,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: Some(wgpu::Face::Back),
                ..Default::default()
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: wgpu::TextureFormat::Depth32Float,
                depth_write_enabled: true,
                depth_compare: wgpu::CompareFunction::Less,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });

        self.chunk_pipeline = Some(pipeline);

        // ---- Transparent pipeline: no depth write, alpha blending, no back-face culling ----
        let pipeline_transparent = self.device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Chunk Pipeline (Transparent)"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: shader,
                entry_point: Some("vs_main"),
                buffers: &[ChunkVertex::desc()],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: shader,
                entry_point: Some("fs_transparent"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: self.surface_format,
                    blend: Some(wgpu::BlendState::ALPHA_BLENDING),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                // No culling for transparent objects — back faces of water/glass
                // need to render for proper semi-transparent appearance.
                cull_mode: None,
                ..Default::default()
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: wgpu::TextureFormat::Depth32Float,
                // CRITICAL: Disable depth write for transparent pass so
                // semi-transparent fragments don't occlude objects behind them.
                // Depth test (Less) still prevents fragments behind opaque
                // geometry from rendering through it.
                depth_write_enabled: false,
                depth_compare: wgpu::CompareFunction::Less,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });

        self.chunk_pipeline_transparent = Some(pipeline_transparent);

        log::info!("[dx12-wm] Chunk pipelines created (opaque + transparent) with format={:?}", self.surface_format);
        eprintln!("[dx12-wm] Chunk pipelines created (opaque + transparent) with format={:?}", self.surface_format);
    }

    /// Create or recreate the entity rendering pipeline.
    fn ensure_entity_pipeline(&mut self) {
        if self.entity_pipeline.is_some() { return; }

        let Some(bgl) = &self.chunk_bind_group_layout else { return; };

        let shader = self.device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Entity Shader"),
            source: wgpu::ShaderSource::Wgsl(ENTITY_SHADER_SRC.into()),
        });

        let pipeline_layout = self.device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Entity PL"),
            bind_group_layouts: &[bgl],
            push_constant_ranges: &[],
        });

        self.entity_pipeline = Some(self.device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Entity Pipeline"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: Some("vs_main"),
                buffers: &[Vertex::desc()],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: self.surface_format,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: Some(wgpu::Face::Back),
                ..Default::default()
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: wgpu::TextureFormat::Depth32Float,
                depth_write_enabled: true,
                depth_compare: wgpu::CompareFunction::Less,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        }));
    }

    /// Create or recreate the particle point sprite pipeline.
    fn ensure_particle_pipeline(&mut self) {
        if self.particle_pipeline.is_some() { return; }

        let Some(bgl) = &self.chunk_bind_group_layout else { return; };

        let shader = self.device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Particle Shader"),
            source: wgpu::ShaderSource::Wgsl(PARTICLE_SHADER_SRC.into()),
        });

        let pipeline_layout = self.device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Particle PL"),
            bind_group_layouts: &[bgl],
            push_constant_ranges: &[],
        });

        self.particle_pipeline = Some(self.device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Particle Pipeline"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: Some("vs_main"),
                buffers: &[ParticleVertex::desc()],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: self.surface_format,
                    blend: Some(wgpu::BlendState::ALPHA_BLENDING),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::PointList,
                cull_mode: None,
                ..Default::default()
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: wgpu::TextureFormat::Depth32Float,
                depth_write_enabled: false,
                depth_compare: wgpu::CompareFunction::Less,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        }));
    }

    // ── Sky dome pipeline (gradient hemisphere) ─────────────────

    /// Create or recreate the sky dome pipeline.
    /// Mesh is allocated once on first call, pipeline is rebuilt on surface format change.
    fn ensure_sky_pipeline(&mut self) {
        // Allocate sky dome mesh on first call
        if self.sky_vb.is_none() {
            let (verts, idxs) = create_sky_dome_mesh();
            self.sky_index_count = idxs.len() as u32;
            let vb_size = (verts.len() * std::mem::size_of::<SkyVertex>()) as u64;
            self.sky_vb = Some(self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Sky VB"),
                size: vb_size,
                usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
                mapped_at_creation: false,
            }));
            self.queue.write_buffer(self.sky_vb.as_ref().unwrap(), 0, bytemuck::cast_slice(&verts));
            let ib_size = (idxs.len() * 2) as u64;
            self.sky_ib = Some(self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Sky IB"),
                size: ib_size,
                usage: wgpu::BufferUsages::INDEX | wgpu::BufferUsages::COPY_DST,
                mapped_at_creation: false,
            }));
            self.queue.write_buffer(self.sky_ib.as_ref().unwrap(), 0, bytemuck::cast_slice(&idxs));
            log::info!("[dx12-wm] Sky dome mesh created: {} verts, {} indices", verts.len(), idxs.len());
        }

        if self.sky_pipeline.is_some() { return; }

        let Some(bgl) = &self.chunk_bind_group_layout else { return; };
        let shader = self.device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Sky Shader"),
            source: wgpu::ShaderSource::Wgsl(SKY_SHADER_SRC.into()),
        });
        let pl = self.device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Sky PL"),
            bind_group_layouts: &[bgl],
            push_constant_ranges: &[],
        });

        self.sky_pipeline = Some(self.device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Sky Pipeline"),
            layout: Some(&pl),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: Some("vs_main"),
                buffers: &[SkyVertex::desc()],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: self.surface_format,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: None,  // No culling — inside view of dome
                ..Default::default()
            },
            depth_stencil: None,  // Sky dome does not write depth — chunks cover it
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        }));
        log::info!("[dx12-wm] Sky dome pipeline created with format={:?}", self.surface_format);
    }

    // ── HUD overlay pipeline (alpha-blended fullscreen quad) ────

    /// Create or recreate the HUD compositing pipeline with the current surface format.
    fn ensure_hud_pipeline(&mut self) {
        if self.hud_pipeline.is_some() { return; }

        let shader = self.device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("HUD Shader"),
            source: wgpu::ShaderSource::Wgsl(TEX_SHADER_SRC.into()),
        });

        // Reuse the same bind group layout as tex_pipeline (texture2D + sampler)
        let bgl = self.tex_pipeline.get_bind_group_layout(0);
        let pl = self.device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("HUD PL"),
            bind_group_layouts: &[&bgl],
            push_constant_ranges: &[],
        });

        let pipeline = self.device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("HUD Pipeline"),
            layout: Some(&pl),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: Some("vs_main"),
                buffers: &[TexVertex::desc()],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: self.surface_format,
                    blend: Some(wgpu::BlendState::ALPHA_BLENDING),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: None,
                ..Default::default()
            },
            depth_stencil: None,
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });
        self.hud_pipeline = Some(pipeline);
        log::info!("[dx12-wm] HUD compositing pipeline created");
    }

    /// Upload GL-captured HUD/UI pixels as a D3D12 texture for compositing.
    pub fn set_hud_pixels(&mut self, data: &[u8], width: u32, height: u32) {
        if width == 0 || height == 0 { return; }
        let size = (width * height * 4) as usize;
        if data.len() < size { return; }

        // D3D12 requires bytes_per_row to be a multiple of 256.
        const ROW_ALIGN: u32 = 256;
        let src_row_bytes = width * 4;
        let dst_row_bytes = ((src_row_bytes + ROW_ALIGN - 1) / ROW_ALIGN) * ROW_ALIGN;
        let padded_size = (dst_row_bytes * height) as usize;

        // Recreate texture if dimensions changed
        let need_new = self.hud_texture.as_ref().map_or(true, |_| {
            self.hud_width != width || self.hud_height != height
        });

        if need_new {
            log::info!("[dx12-wm] Creating new HUD texture {}x{} (row {}→{} padded)",
                width, height, src_row_bytes, dst_row_bytes);
            let tex = self.device.create_texture(&wgpu::TextureDescriptor {
                label: Some("HUD Texture"),
                size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                mip_level_count: 1,
                sample_count: 1,
                dimension: wgpu::TextureDimension::D2,
                format: wgpu::TextureFormat::Rgba8UnormSrgb,
                usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
                view_formats: &[],
            });

            let view = tex.create_view(&wgpu::TextureViewDescriptor::default());
            let bgl = self.tex_pipeline.get_bind_group_layout(0);
            let bind_group = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
                label: Some("HUD Bind Group"),
                layout: &bgl,
                entries: &[
                    wgpu::BindGroupEntry {
                        binding: 0,
                        resource: wgpu::BindingResource::TextureView(&view),
                    },
                    wgpu::BindGroupEntry {
                        binding: 1,
                        resource: wgpu::BindingResource::Sampler(&self.tex_sampler),
                    },
                ],
            });

            self.hud_texture = Some(tex);
            self.hud_bind_group = Some(bind_group);
            self.hud_width = width;
            self.hud_height = height;
            log::info!("[dx12-wm] HUD texture + bind_group created OK");
        }

        // Always ensure the pipeline exists — init_surface() sets it to None,
        // and dimension may not have changed, so the ensure must run outside
        // the need_new block to actually re-create the pipeline.
        self.ensure_hud_pipeline();

        // Upload pixel data with D3D12-aligned row pitch
        let mut padded = Vec::with_capacity(padded_size);
        padded.resize(padded_size, 0u8);
        for row in 0..height as usize {
            let src_start = row * src_row_bytes as usize;
            let dst_start = row * dst_row_bytes as usize;
            if src_start + src_row_bytes as usize <= data.len() && dst_start + src_row_bytes as usize <= padded.len() {
                padded[dst_start..dst_start + src_row_bytes as usize]
                    .copy_from_slice(&data[src_start..src_start + src_row_bytes as usize]);
            }
        }

        if let Some(ref tex) = self.hud_texture {
            self.queue.write_texture(
                wgpu::ImageCopyTexture {
                    texture: tex,
                    mip_level: 0,
                    origin: wgpu::Origin3d::ZERO,
                    aspect: wgpu::TextureAspect::All,
                },
                &padded,
                wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(dst_row_bytes),
                    rows_per_image: Some(height),
                },
                wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
            );
        }
    }

    /// Draw the HUD overlay as a fullscreen textured quad with alpha blending.
    fn draw_hud_overlay<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        let Some(pipeline) = &self.hud_pipeline else { return; };
        let Some(bg) = &self.hud_bind_group else { return; };
        rp.set_pipeline(pipeline);
        rp.set_bind_group(0, bg, &[]);
        rp.set_vertex_buffer(0, self.fs_quad_vb.slice(..));
        rp.set_index_buffer(self.fs_quad_ib.slice(..), wgpu::IndexFormat::Uint16);
        rp.draw_indexed(0..6, 0, 0..1);
    }

    // ── Surface mode: render directly to swapchain ────────────────

    fn render_surface(&mut self) {
        // Phase 11e: apply any pending chunk mesh changes (merged buffers)
        // before deciding what to render this frame.
        self.prepare_chunk_batch();

        let has_frame = self.tex_bind_group.is_some();
        let has_chunks = self.has_chunk_geometry;
        let has_hud = self.hud_bind_group.is_some();

        let surface = self.surface.as_ref().unwrap();

        // If a resize is pending, reconfigure the swapchain dimensions.
        // Depth texture is NOT recreated here — it is created AFTER
        // get_current_texture() succeeds, using the actual frame texture
        // dimensions. This guarantees depth and color attachments always
        // match, avoiding "Attachments have differing sizes" validation
        // errors during window resize transitions.
        if self.resize_pending {
            if let Some(config) = &self.surface_config {
                log::info!("[dx12-wm] Applying pending surface reconfig to {}x{}", config.width, config.height);
                surface.configure(&self.device, config);
            }
            self.resize_pending = false;
        }

        // Get surface frame
        let frame = match surface.get_current_texture() {
            Ok(f) => {
                // Ensure depth texture matches the actual swapchain image size.
                // This prevents mismatches when the swapchain hasn't fully
                // transitioned to the new config size yet.
                let actual = f.texture.size();
                let needs_recreate = match &self.surface_depth {
                    Some(depth) => depth.size().width != actual.width
                                || depth.size().height != actual.height,
                    None => true,
                };
                if needs_recreate {
                    log::info!("[dx12-wm] Recreating depth texture to match swapchain {}x{}",
                        actual.width, actual.height);
                    self.surface_depth = Some(make_depth_texture(&self.device, actual.width, actual.height));
                    self.width = actual.width;
                    self.height = actual.height;
                }
                f
            }
            Err(wgpu::SurfaceError::Outdated | wgpu::SurfaceError::Lost) => {
                log::info!("[dx12-wm] Surface lost/outdated, reconfiguring");
                if let Some(config) = &self.surface_config {
                    surface.configure(&self.device, config);
                    self.surface_depth = Some(make_depth_texture(&self.device, self.width, self.height));
                }
                return;
            }
            Err(wgpu::SurfaceError::Timeout) => {
                log::warn!("[dx12-wm] Surface timeout — GPU TDR may have occurred; skipping frame");
                return;
            }
            Err(e) => {
                log::error!("[dx12-wm] Surface error: {:?}, falling back to offscreen", e);
                self.surface = None;
                self.surface_depth = None;
                self.surface_config = None;
                self.surface_hwnd = 0;
                log::info!("[dx12-wm] Fallen back to offscreen rendering");
                return;
            }
        };

        let view = frame.texture.create_view(&wgpu::TextureViewDescriptor::default());

        let mut encoder = self.device.create_command_encoder(
            &wgpu::CommandEncoderDescriptor { label: Some("RenderSurface") },
        );

        // Wrap the fallible rendering in catch_unwind so we always present
        // the frame — otherwise the swapchain image stays acquired forever.
        let render_result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            if has_chunks {
                // Phase 7: Render native MC chunk geometry with D3D12
                self.camera_mvp = mat4_lerp(&self.camera_prev, &self.camera_target, LERP_FACTOR);
                self.camera_prev = self.camera_mvp;
                write_camera_uniform(&self.queue, &self.uniform_buffer, &self.camera_mvp, &self.camera_pos, &self.fog_color, &self.sky_color, &[self.width as f32, self.height as f32]);

                let depth_view = self.surface_depth
                    .as_ref()
                    .expect("surface_depth must be created in init_surface")
                    .create_view(&wgpu::TextureViewDescriptor::default());

                // Ensure sky pipeline exists for this surface format
                self.ensure_sky_pipeline();

                // Pass 0: Sky dome (gradient hemisphere, no depth)
                {
                    // Clear with the dynamic MC sky color so any pixels the
                    // dome doesn't cover (e.g. below the horizon line) still
                    // match Minecraft's sky instead of a hardcoded blue.
                    let sky = self.sky_color;
                    let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                        label: Some("Sky Dome Pass"),
                        color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                            view: &view,
                            resolve_target: None,
                            ops: wgpu::Operations {
                                load: wgpu::LoadOp::Clear(wgpu::Color {
                                    r: sky[0] as f64, g: sky[1] as f64, b: sky[2] as f64, a: 1.0,
                                }),
                                store: wgpu::StoreOp::Store,
                            },
                        })],
                        depth_stencil_attachment: None,
                        timestamp_writes: None,
                        occlusion_query_set: None,
                    });
                    self.draw_sky_dome(&mut rp);
                }

                // Pass 1: Render world (chunks + entities + particles) with depth
                {
                    let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                        label: Some("Surface Pass (Chunks)"),
                        color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                            view: &view,
                            resolve_target: None,
                            ops: wgpu::Operations {
                                load: wgpu::LoadOp::Load,  // Preserve sky dome output
                                store: wgpu::StoreOp::Store,
                            },
                        })],
                        depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                            view: &depth_view,
                            depth_ops: Some(wgpu::Operations {
                                load: wgpu::LoadOp::Clear(1.0),
                                store: wgpu::StoreOp::Store,
                            }),
                            stencil_ops: None,
                        }),
                        timestamp_writes: None,
                        occlusion_query_set: None,
                    });

                    self.draw_chunks(&mut rp);
                    self.draw_entities(&mut rp);
                    self.draw_particles(&mut rp);
                }
                // Pass 2: HUD overlay (load world output, no depth, alpha blending)
                if has_hud {
                    {
                        let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                            label: Some("HUD Overlay Pass"),
                            color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                                view: &view,
                                resolve_target: None,
                                ops: wgpu::Operations {
                                    load: wgpu::LoadOp::Load,
                                    store: wgpu::StoreOp::Store,
                                },
                            })],
                            depth_stencil_attachment: None,
                            timestamp_writes: None,
                            occlusion_query_set: None,
                        });
                        self.draw_hud_overlay(&mut rp);
                    }
                }
            } else if has_frame {
                // Textured fullscreen quad: display GL framebuffer capture
                {
                    let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                        label: Some("Surface Pass (Textured)"),
                        color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                            view: &view,
                            resolve_target: None,
                            ops: wgpu::Operations {
                                load: wgpu::LoadOp::Clear(wgpu::Color { r: 0.0, g: 0.0, b: 0.0, a: 1.0 }),
                                store: wgpu::StoreOp::Store,
                            },
                        })],
                        depth_stencil_attachment: None,
                        timestamp_writes: None,
                        occlusion_query_set: None,
                    });
                    rp.set_pipeline(&self.tex_pipeline);
                    rp.set_bind_group(0, self.tex_bind_group.as_ref().unwrap(), &[]);
                    rp.set_vertex_buffer(0, self.fs_quad_vb.slice(..));
                    rp.set_index_buffer(self.fs_quad_ib.slice(..), wgpu::IndexFormat::Uint16);
                    rp.draw_indexed(0..6, 0, 0..1);
                }
            } else {
                // Fallback: 3D test scene (plane + cubes)
                self.camera_mvp = mat4_lerp(&self.camera_prev, &self.camera_target, LERP_FACTOR);
                self.camera_prev = self.camera_mvp;
                write_camera_uniform(&self.queue, &self.uniform_buffer, &self.camera_mvp, &self.camera_pos, &self.fog_color, &self.sky_color, &[self.width as f32, self.height as f32]);

                {
                    let depth_view = self.surface_depth
                        .as_ref()
                        .expect("surface_depth must be created in init_surface")
                        .create_view(&wgpu::TextureViewDescriptor::default());

                    let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                        label: Some("Surface Pass (3D)"),
                        color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                            view: &view,
                            resolve_target: None,
                            ops: wgpu::Operations {
                                load: wgpu::LoadOp::Clear(wgpu::Color {
                                    r: 0.53, g: 0.81, b: 0.92, a: 1.0,
                                }),
                                store: wgpu::StoreOp::Store,
                            },
                        })],
                        depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                            view: &depth_view,
                            depth_ops: Some(wgpu::Operations {
                                load: wgpu::LoadOp::Clear(1.0),
                                store: wgpu::StoreOp::Store,
                            }),
                            stencil_ops: None,
                        }),
                        timestamp_writes: None,
                        occlusion_query_set: None,
                    });

                    self.draw_scene(&mut rp);
                }
            }

            self.queue.submit(Some(encoder.finish()));
        }));

        // Always present the frame, even if the render block panicked.
        // This releases the swapchain image so subsequent frames can acquire.
        match render_result {
            Ok(()) => {}
            Err(e) => {
                let msg = if let Some(s) = e.downcast_ref::<&str>() {
                    s.to_string()
                } else if let Some(s) = e.downcast_ref::<String>() {
                    s.clone()
                } else {
                    "unknown panic".to_string()
                };
                log::error!("[dx12-wm] render_surface panicked: {}", msg);
            }
        }
        frame.present();
    }

    // ── Offscreen mode: triple-buffer readback ────────────────────

    fn render_offscreen(&mut self) -> Vec<u8> {
        // Phase 11e: keep merged chunk buffers in sync even when rendering
        // offscreen (title screen / init), so the first surface frame is ready.
        self.prepare_chunk_batch();

        let w = self.width;
        let h = self.height;
        if w == 0 || h == 0 {
            return Vec::new();
        }

        self.camera_mvp = mat4_lerp(&self.camera_prev, &self.camera_target, LERP_FACTOR);
        self.camera_prev = self.camera_mvp;

        write_camera_uniform(&self.queue, &self.uniform_buffer, &self.camera_mvp, &self.camera_pos, &self.fog_color, &self.sky_color, &[w as f32, h as f32]);

        let slot = &self.slots[self.idx];
        let row_aligned = aligned_row(w);

        let mut encoder = self.device.create_command_encoder(
            &wgpu::CommandEncoderDescriptor { label: Some("Render") },
        );

        {
            let color_view = slot.color.create_view(&wgpu::TextureViewDescriptor::default());
            let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Main Pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &color_view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.53, g: 0.81, b: 0.92, a: 1.0,
                        }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                    view: &slot.depth_view,
                    depth_ops: Some(wgpu::Operations {
                        load: wgpu::LoadOp::Clear(1.0),
                        store: wgpu::StoreOp::Store,
                    }),
                    stencil_ops: None,
                }),
                timestamp_writes: None,
                occlusion_query_set: None,
            });

            self.draw_scene(&mut rp);
            self.draw_entities(&mut rp);
            self.draw_particles(&mut rp);
        }

        encoder.copy_texture_to_buffer(
            wgpu::ImageCopyTexture {
                texture: &slot.color,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            wgpu::ImageCopyBuffer {
                buffer: &slot.staging,
                layout: wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(row_aligned),
                    rows_per_image: Some(h),
                },
            },
            wgpu::Extent3d { width: w, height: h, depth_or_array_layers: 1 },
        );

        self.queue.submit(Some(encoder.finish()));
        self.device.poll(wgpu::Maintain::Poll);

        {
            let slice = slot.staging.slice(..);
            let (tx, rx) = mpsc::channel();
            slice.map_async(wgpu::MapMode::Read, move |result| {
                let _ = tx.send(result);
            });
            self.device.poll(wgpu::Maintain::Poll);
            self.pending_rx[self.idx] = Some(rx);
        }

        let read_idx = (self.idx + RING_SIZE - 1) % RING_SIZE;
        let mut pixels = Vec::new();

        if let Some(rx) = self.pending_rx[read_idx].take() {
            match rx.recv_timeout(std::time::Duration::from_millis(0)) {
                Ok(Ok(())) => {
                    let data = self.slots[read_idx].staging.slice(..).get_mapped_range();
                    let row_aligned_usize = row_aligned as usize;
                    let actual_row = (w * 4) as usize;
                    let mut new_pixels = Vec::with_capacity(actual_row * h as usize);
                    for row in 0..h as usize {
                        let start = row * row_aligned_usize;
                        let end = start + actual_row;
                        new_pixels.extend_from_slice(&data[start..end]);
                    }
                    drop(data);
                    self.slots[read_idx].staging.unmap();
                    pixels = new_pixels;
                }
                _ => {
                    self.pending_rx[read_idx] = Some(rx);
                }
            }
        }

        if pixels.is_empty() && !self.prev_pixels.is_empty() {
            pixels = self.prev_pixels.clone();
        }
        if !pixels.is_empty() {
            self.prev_pixels = pixels.clone();
        }

        self.idx = (self.idx + 1) % RING_SIZE;
        pixels
    }
}

// ── Phase 11f: frustum culling unit tests ──────────────────────────────

#[cfg(test)]
mod frustum_tests {
    use super::*;

    /// Build a D3D-style perspective matrix (row-major storage, z ∈ [0, w]).
    /// Matches JOML: perspective(fovy, aspect, zNear, zFar, zZeroToOne=true).
    pub(crate) fn d3d_perspective(fovy_rad: f32, aspect: f32, zn: f32, zf: f32) -> [[f32; 4]; 4] {
        let f = 1.0 / (fovy_rad / 2.0).tan();
        let mut m = [[0.0f32; 4]; 4];
        m[0][0] = f / aspect;
        m[1][1] = f;
        m[2][2] = zf / (zf - zn);
        m[2][3] = -zf * zn / (zf - zn);
        m[3][2] = 1.0; // clip.w = z
        m
    }

    fn point_in_frustum(v: [f32; 3], planes: &[[f32; 4]; 6]) -> bool {
        for p in planes {
            let d = p[0] * v[0] + p[1] * v[1] + p[2] * v[2] + p[3];
            if d < 0.0 {
                return false;
            }
        }
        true
    }

    #[test]
    fn planes_are_normalized() {
        let m = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        let planes = extract_frustum_planes(&m);
        assert_eq!(planes.len(), 6);
        for p in planes.iter() {
            let len = (p[0] * p[0] + p[1] * p[1] + p[2] * p[2]).sqrt();
            assert!(
                (len - 1.0).abs() < 1e-5,
                "plane not normalized: {:?} len={}",
                p,
                len
            );
        }
    }

    #[test]
    fn interior_point_passes_all_planes() {
        let m = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        let planes = extract_frustum_planes(&m);
        // Dead center of the frustum, z=5
        assert!(point_in_frustum([0.0, 0.0, 5.0], &planes));
        // Corner-ish point still inside
        assert!(point_in_frustum([1.0, -0.6, 5.0], &planes));
        // Just past the near plane
        assert!(point_in_frustum([0.0, 0.0, 0.1], &planes));
    }

    #[test]
    fn outside_points_are_rejected() {
        let m = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        let planes = extract_frustum_planes(&m);
        // Beyond far plane
        assert!(!point_in_frustum([0.0, 0.0, 2000.0], &planes));
        // Behind the camera (z < zNear)
        assert!(!point_in_frustum([0.0, 0.0, -1.0], &planes));
        // Far right of the view cone
        assert!(!point_in_frustum([500.0, 0.0, 5.0], &planes));
        // Above the view cone
        assert!(!point_in_frustum([0.0, 500.0, 5.0], &planes));
        // Far left
        assert!(!point_in_frustum([-500.0, 0.0, 5.0], &planes));
    }

    #[test]
    fn aabb_inside_and_outside() {
        let m = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        let planes = extract_frustum_planes(&m);

        // Box fully inside the frustum (near the camera center)
        assert!(aabb_in_frustum([-1.0, -1.0, 4.0], [1.0, 1.0, 6.0], &planes));
        // Box straddling the camera (contains origin) — visible
        assert!(aabb_in_frustum([-10.0, -10.0, -10.0], [10.0, 10.0, 10.0], &planes));
        // Box fully outside (far corner of the world)
        assert!(!aabb_in_frustum([900.0, 900.0, 900.0], [910.0, 910.0, 910.0], &planes));
        // Box entirely behind the camera
        assert!(!aabb_in_frustum([-10.0, -10.0, -20.0], [10.0, 10.0, -10.1], &planes));
        // Box far to the right
        assert!(!aabb_in_frustum([500.0, -5.0, 4.0], [600.0, 5.0, 6.0], &planes));
    }

    #[test]
    fn zero_planes_pass_every_box() {
        // draw_chunks uses all-zero planes when the camera matrix is IDENTITY
        // (cull_enabled = camera_mvp[3][2] != 0.0 is false) — nothing may be culled.
        let planes = [[0.0f32; 4]; 6];
        assert!(aabb_in_frustum([1000.0, 1000.0, 1000.0], [2000.0, 2000.0, 2000.0], &planes));
        assert!(aabb_in_frustum([-9999.0, -9999.0, -9999.0], [-1.0, -1.0, -1.0], &planes));
    }

    #[test]
    fn camera_matrix_validity_heuristic() {
        // A real projection matrix has row 4 z-component = 1 (clip.w = z);
        // the initial IDENTITY has 0, which disables culling.
        let id = [[0.0f32; 4]; 4];
        assert_eq!(id[3][2], 0.0);
        let proj = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        assert_ne!(proj[3][2], 0.0);
    }

    #[test]
    fn transpose_roundtrip_is_identity() {
        let m = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        assert_eq!(transpose4(&transpose4(&m)), m);
    }

    #[test]
    fn column_major_input_needs_transpose_for_planes() {
        // JNI stores the JOML column-major float[16] as-is, so the in-memory
        // [[f32; 4]; 4] is the transpose of the true world→clip matrix.
        // Plane extraction must run on the transposed form.
        let true_mvp = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        let stored = transpose4(&true_mvp); // what Rust sees at runtime

        // Regression: before the fix, planes came from the transposed matrix
        // and looking up culled every section (ground vanished).
        let planes_bad = extract_frustum_planes(&stored);
        let planes_ok = extract_frustum_planes(&transpose4(&stored));

        assert_eq!(planes_ok, extract_frustum_planes(&true_mvp));
        assert_ne!(planes_bad, planes_ok);

        // Box directly ahead of the camera must stay visible…
        assert!(aabb_in_frustum([-1.0, -1.0, 4.0], [1.0, 1.0, 6.0], &planes_ok));
        // …and a box behind the camera must be culled.
        assert!(!aabb_in_frustum([-1.0, -1.0, -20.0], [1.0, 1.0, -10.1], &planes_ok));
    }
}

#[cfg(test)]
mod batch_tests {
    use super::*;
    use super::frustum_tests::d3d_perspective;

    /// Build a fake mesh whose merged-buffer ranges are preset (as if
    /// rebuild_chunk_buffers() had already assigned them).
    fn make_mesh(base_vertex: u32, index_offset: u32, index_count: u32) -> ChunkMesh {
        ChunkMesh {
            vertices: Vec::new(),
            indices: Vec::new(),
            base_vertex,
            index_offset,
            index_count,
        }
    }

    #[test]
    fn indirect_args_layout_matches_dx12() {
        // D3D12_DRAW_INDEXED_ARGUMENTS must be exactly 20 bytes little-endian:
        // IndexCountPerInstance, InstanceCount, StartIndexLocation,
        // BaseVertexLocation, StartInstanceLocation.
        assert_eq!(std::mem::size_of::<DrawIndexedIndirectArgs>(), 20);
        let a = DrawIndexedIndirectArgs {
            index_count: 6,
            instance_count: 1,
            first_index: 12,
            base_vertex: 4,
            first_instance: 0,
        };
        let bytes: [u8; 20] = bytemuck::cast(a);
        assert_eq!(&bytes[0..4], &6u32.to_le_bytes());
        assert_eq!(&bytes[4..8], &1u32.to_le_bytes());
        assert_eq!(&bytes[8..12], &12u32.to_le_bytes());
        assert_eq!(&bytes[12..16], &(4i32).to_le_bytes());
        assert_eq!(&bytes[16..20], &0u32.to_le_bytes());
    }

    #[test]
    fn batch_culls_outside_sections_and_preserves_ranges() {
        let m = d3d_perspective(70.0f32.to_radians(), 16.0 / 9.0, 0.05, 1000.0);
        let planes = extract_frustum_planes(&m);

        let mut sections: std::collections::HashMap<(i32, i32, i32), Vec<ChunkMesh>> =
            std::collections::HashMap::new();
        // Section containing the camera origin — visible, two meshes.
        sections.insert((0, 0, 0), vec![
            make_mesh(0, 0, 36),
            make_mesh(36, 36, 72),
        ]);
        // Far outside the frustum — must be culled.
        sections.insert((5000, 0, 0), vec![make_mesh(100, 100, 36)]);

        let (draws, visible, culled) = collect_visible_draws(sections.iter(), &planes);
        assert_eq!(visible, 1);
        assert_eq!(culled, 1);
        assert_eq!(draws.len(), 2);

        assert_eq!(draws[0].index_count, 36);
        assert_eq!(draws[0].first_index, 0);
        assert_eq!(draws[0].base_vertex, 0);
        assert_eq!(draws[1].index_count, 72);
        assert_eq!(draws[1].first_index, 36);
        assert_eq!(draws[1].base_vertex, 36);
        for d in &draws {
            assert_eq!(d.instance_count, 1);
            assert_eq!(d.first_instance, 0);
        }
    }

    #[test]
    fn batch_zero_planes_include_every_section() {
        // All-zero planes (IDENTITY camera) must never cull anything.
        let planes = [[0.0f32; 4]; 6];
        let mut sections: std::collections::HashMap<(i32, i32, i32), Vec<ChunkMesh>> =
            std::collections::HashMap::new();
        sections.insert((0, 0, 0), vec![make_mesh(0, 0, 6)]);
        sections.insert((10000, 0, 0), vec![make_mesh(10, 10, 12)]);

        let (draws, visible, culled) = collect_visible_draws(sections.iter(), &planes);
        assert_eq!(visible, 2);
        assert_eq!(culled, 0);
        assert_eq!(draws.len(), 2);
    }

    #[test]
    fn batch_empty_world_produces_no_draws() {
        let planes = [[0.0f32; 4]; 6];
        let sections: std::collections::HashMap<(i32, i32, i32), Vec<ChunkMesh>> =
            std::collections::HashMap::new();
        let (draws, visible, culled) = collect_visible_draws(sections.iter(), &planes);
        assert!(draws.is_empty());
        assert_eq!(visible, 0);
        assert_eq!(culled, 0);
    }
}

#[cfg(test)]
mod upload_tests {
    use super::*;

    const EPS: f32 = 1e-6;

    fn near(a: f32, b: f32) -> bool {
        (a - b).abs() < EPS
    }

    /// One entity: center (10,20,30), size w=2 h=4 d=6, color (1, 0.5, 0).
    fn one_entity_data() -> [f32; 9] {
        [10.0, 20.0, 30.0, 2.0, 4.0, 6.0, 1.0, 0.5, 0.0]
    }

    #[test]
    fn entity_box_geometry_matches_bbox() {
        let v = WmRenderer::build_entity_vertices(&one_entity_data()).expect("valid data");
        // 6 faces × 2 tris × 3 verts = 36 non-indexed vertices.
        assert_eq!(v.len(), 36);

        // bbox half extents: sx=1, sy=2, sz=3.
        // First vertex of the front face: (min_x, min_y, max_z).
        assert!(near(v[0].position[0], 9.0) && near(v[0].position[1], 18.0) && near(v[0].position[2], 33.0));

        // Every vertex must lie on the box surface (one axis at a bound) and
        // carry the entity color.
        for vert in &v {
            assert!(near(vert.color[0], 1.0) && near(vert.color[1], 0.5) && near(vert.color[2], 0.0));
            let px = vert.position[0];
            let py = vert.position[1];
            let pz = vert.position[2];
            let on_surface = near(px, 9.0) || near(px, 11.0)
                || near(py, 18.0) || near(py, 22.0)
                || near(pz, 27.0) || near(pz, 33.0);
            assert!(on_surface, "vertex {:?} not on box surface", vert.position);
        }
    }

    #[test]
    fn entity_builder_rejects_invalid_counts() {
        assert!(WmRenderer::build_entity_vertices(&[]).is_none());

        // 257 entities (> 256 cap) → rejected.
        let mut big = Vec::with_capacity(257 * 9);
        for _ in 0..257 {
            big.extend_from_slice(&one_entity_data());
        }
        assert!(WmRenderer::build_entity_vertices(&big).is_none());

        // Exactly 256 → accepted.
        let mut maxed = Vec::with_capacity(256 * 9);
        for _ in 0..256 {
            maxed.extend_from_slice(&one_entity_data());
        }
        assert_eq!(WmRenderer::build_entity_vertices(&maxed).unwrap().len(), 256 * 36);
    }

    #[test]
    fn entity_builder_clamps_colors_and_min_size() {
        // Zero size → clamped to 0.01 half-extent; out-of-range color → clamped to [0,1].
        let data = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -1.0, 3.0];
        let v = WmRenderer::build_entity_vertices(&data).expect("valid data");
        assert_eq!(v.len(), 36);
        assert!(near(v[0].position[0], -0.01) && near(v[0].position[1], -0.01) && near(v[0].position[2], 0.01));
        assert!(near(v[0].color[0], 1.0) && near(v[0].color[1], 0.0) && near(v[0].color[2], 1.0));
    }

    #[test]
    fn particle_builder_builds_points() {
        let data = [1.0, 2.0, 3.0, 0.5, 1.0, 0.0, 0.0, 1.0];
        let p = WmRenderer::build_particle_vertices(&data).expect("valid data");
        // One particle → 6 quad vertices (2 triangles).
        assert_eq!(p.len(), 6);
        assert!(near(p[0].position[0], 1.0) && near(p[0].position[1], 2.0) && near(p[0].position[2], 3.0));
        // size < 1 → clamped to 1.0
        assert!(near(p[0].size, 1.0));
        assert!(near(p[0].color[0], 1.0) && near(p[0].color[1], 0.0) && near(p[0].color[2], 0.0) && near(p[0].color[3], 1.0));
        // Corners cover the full quad in [-0.5, 0.5]².
        let min_x = p.iter().map(|v| v.corner[0]).fold(f32::INFINITY, f32::min);
        let max_x = p.iter().map(|v| v.corner[0]).fold(f32::NEG_INFINITY, f32::max);
        assert!(near(min_x, -0.5) && near(max_x, 0.5));
    }

    #[test]
    fn particle_builder_rejects_invalid_counts() {
        assert!(WmRenderer::build_particle_vertices(&[]).is_none());

        // 2049 particles (> 2048 cap) → rejected.
        let mut big = Vec::with_capacity(2049 * 8);
        for _ in 0..2049 {
            big.extend_from_slice(&[1.0, 2.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0]);
        }
        assert!(WmRenderer::build_particle_vertices(&big).is_none());

        // Exactly 2048 → accepted, 2048*6 quad vertices.
        let mut maxed = Vec::with_capacity(2048 * 8);
        for _ in 0..2048 {
            maxed.extend_from_slice(&[1.0, 2.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0]);
        }
        assert_eq!(WmRenderer::build_particle_vertices(&maxed).unwrap().len(), 2048 * 6);
    }
}
