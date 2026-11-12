// Persistent D3D11 device, swapchain, resources and render-pass state for the
// additive Minecraft 1.21.11 backend.
//
// The Java side owns the Minecraft-facing objects (Dx11Device / Dx11Texture /
// ...).  Native handles are opaque pointers to the structs below.  All methods
// are called on the render thread; the D3D11 immediate context is used, so a
// command takes effect as soon as the call returns.
#pragma once

#include "dx11_common.h"

#include <cstdint>
#include <string>
#include <vector>

namespace dx11mc {

// D3D11 has no triangle-fan topology.  The Java layer passes this sentinel
// (the old OpenGL mode ordinal) and the draw path expands the fan into a
// temporary triangle-list index buffer.
constexpr D3D11_PRIMITIVE_TOPOLOGY kTopologyTriangleFan =
    static_cast<D3D11_PRIMITIVE_TOPOLOGY>(6);

struct Device;
struct Texture;
struct TextureView;
struct Buffer;
struct Sampler;
struct Pipeline;
struct RenderPass;
struct Fence;
struct Query;

// ---------------------------------------------------------------------------
// Pipeline creation description (already translated by the Java layer).
// ---------------------------------------------------------------------------
struct InputElement {
    std::string semantic;
    UINT semanticIndex = 0;
    DXGI_FORMAT format = DXGI_FORMAT_UNKNOWN;
    UINT offset = 0;
};

struct PipelineDesc {
    std::string name;
    std::string vertexHlsl;
    std::string fragmentHlsl;
    std::vector<InputElement> inputs;
    UINT stride = 0;
    D3D11_PRIMITIVE_TOPOLOGY topology = D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST;

    bool blend = false;
    D3D11_BLEND srcColor = D3D11_BLEND_ONE;
    D3D11_BLEND dstColor = D3D11_BLEND_ZERO;
    D3D11_BLEND srcAlpha = D3D11_BLEND_ONE;
    D3D11_BLEND dstAlpha = D3D11_BLEND_ZERO;

    bool depthTest = true;
    bool depthWrite = true;
    D3D11_COMPARISON_FUNC depthFunc = D3D11_COMPARISON_LESS_EQUAL;

    bool cull = true;
    D3D11_FILL_MODE fillMode = D3D11_FILL_SOLID;
    INT depthBias = 0;
    FLOAT slopeScaledDepthBias = 0.0f;
    UINT colorWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;

    // Sizes (in bytes) of the uniform-buffer slots (b0..bN-1) declared by the
    // pipeline.  Feature level 10.x has no *SetConstantBuffers1, so each slot
    // gets its own DYNAMIC constant buffer that is refilled from the source
    // buffer's CPU shadow on every setUniform.
    std::vector<UINT> uniformBufferSizes;
};

// ---------------------------------------------------------------------------
// Resources.
// ---------------------------------------------------------------------------
struct Texture {
    Device* device = nullptr;
    ComPtr<ID3D11Texture2D> texture;
    ComPtr<ID3D11ShaderResourceView> srv;
    ComPtr<ID3D11RenderTargetView> rtv;
    ComPtr<ID3D11DepthStencilView> dsv;
    // Cached staging texture used for partial writeToTexture calls.
    ComPtr<ID3D11Texture2D> uploadStaging;
    UINT uploadStagingMip = UINT_MAX;
    DXGI_FORMAT resourceFormat = DXGI_FORMAT_UNKNOWN;
    DXGI_FORMAT srvFormat = DXGI_FORMAT_UNKNOWN;
    DXGI_FORMAT dsvFormat = DXGI_FORMAT_UNKNOWN;
    UINT width = 0;
    UINT height = 0;
    UINT depthOrLayers = 1;
    UINT mipLevels = 1;
    UINT bytesPerPixel = 4;
    int usage = 0;
    bool depth = false;
    bool cube = false;
    bool closed = false;
};

struct TextureView {
    Texture* texture = nullptr;
    ComPtr<ID3D11ShaderResourceView> srv;
    // Per-view attachment views.  A view into a mip level must render to that
    // mip (TextureAtlas animates sprites through mip views).
    ComPtr<ID3D11RenderTargetView> rtv;
    ComPtr<ID3D11DepthStencilView> dsv;
    UINT baseMip = 0;
    UINT mipLevels = 1;
    bool closed = false;
};

struct Buffer {
    Device* device = nullptr;
    ComPtr<ID3D11Buffer> buffer;
    UINT size = 0;          // logical size in bytes
    UINT capacity = 0;      // D3D11 buffer size (rounded up)
    int usage = 0;
    bool hasShadow = false;
    std::vector<uint8_t> shadow;
    uint64_t version = 1;

    // Map state.  A D3D11 map covers the whole buffer; Java sees a slice.
    bool mapped = false;
    bool mappedWrite = false;
    D3D11_MAP mapType = D3D11_MAP_READ;
    void* mappedPtr = nullptr;
    UINT mappedOffset = 0;
    UINT mappedSize = 0;

    // Lazily created staging buffer for readback / partial writes of
    // non-shadow buffers.
    ComPtr<ID3D11Buffer> staging;
    bool mappedViaStaging = false;
    bool mappedShadowOnly = false;

    // Emulated typed-buffer texture (D3D11 feature level 10.x has no Buffer<T>).
    ComPtr<ID3D11Texture2D> texelTexture;
    ComPtr<ID3D11ShaderResourceView> texelSrv;
    uint64_t texelVersion = 0;
    UINT texelWidth = 0;
    UINT texelHeight = 0;
    bool closed = false;
};

struct Sampler {
    Device* device = nullptr;
    ComPtr<ID3D11SamplerState> sampler;
    bool closed = false;
};

struct Pipeline {
    Device* device = nullptr;
    ComPtr<ID3D11VertexShader> vs;
    ComPtr<ID3D11PixelShader> ps;
    ComPtr<ID3D11InputLayout> inputLayout;
    ComPtr<ID3D11BlendState> blendState;
    ComPtr<ID3D11DepthStencilState> depthState;
    ComPtr<ID3D11RasterizerState> rasterState;
    D3D11_PRIMITIVE_TOPOLOGY topology = D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST;
    UINT stride = 0;
    std::vector<ComPtr<ID3D11Buffer>> constantBuffers;
    std::vector<UINT> constantBufferSizes;
    bool closed = false;
};

struct RenderPass {
    Device* device = nullptr;
    Texture* colorTexture = nullptr;
    Texture* depthTexture = nullptr;
    TextureView* colorView = nullptr;
    Pipeline* pipeline = nullptr;
    Buffer* vertexBuffer = nullptr;
    Buffer* indexBuffer = nullptr;
    DXGI_FORMAT indexFormat = DXGI_FORMAT_R32_UINT;
    UINT targetWidth = 0;
    UINT targetHeight = 0;
    bool scissorEnabled = false;
    int scissorX = 0;
    int scissorY = 0;
    int scissorWidth = 0;
    int scissorHeight = 0;
    UINT boundSrvCount = 0;
    bool closed = false;

    // Scratch index buffer for emulated triangle fans.
    ComPtr<ID3D11Buffer> fanIndexBuffer;
    UINT fanIndexCapacity = 0;
};

struct Fence {
    Device* device = nullptr;
    ComPtr<ID3D11Query> query;
    bool closed = false;
};

// GPU timer query (D3D11 timestamp disjoint).  Used only by the debug
// profiler; TimerQuery.FrameProfile polls getValue() until it is present.
struct Query {
    Device* device = nullptr;
    ComPtr<ID3D11Query> disjoint;
    ComPtr<ID3D11Query> start;
    ComPtr<ID3D11Query> end;
    bool begun = false;
    bool ended = false;
    bool hasResult = false;
    int64_t resultNanos = 0;
    bool closed = false;
};

struct Device {
    ComPtr<ID3D11Device> device;
    ComPtr<ID3D11DeviceContext> context;
    ComPtr<ID3D11DeviceContext1> context1;
    ComPtr<IDXGIFactory1> factory;
    ComPtr<IDXGIAdapter1> adapter;

    ComPtr<IDXGISwapChain> swapChain;
    ComPtr<ID3D11Texture2D> backBuffer;
    ComPtr<ID3D11RenderTargetView> backBufferRtv;
    HWND hwnd = nullptr;
    UINT swapWidth = 0;
    UINT swapHeight = 0;
    bool vsync = true;
    bool swapOccluded = false;
    bool hwndIsWindow = false;
    UINT presentCount = 0;
    long lastPresentHr = 0;
    UINT blitCount = 0;

    D3D_FEATURE_LEVEL featureLevel = D3D_FEATURE_LEVEL_9_1;
    bool clearViewSupported = false;
    std::string adapterName;
    std::string vendorName;
    std::string versionText;
    UINT vendorId = 0;
    UINT deviceId = 0;
    UINT maxTextureSize = 8192;

    D3DCompiler compiler;

    // Fullscreen present blit (samples the main render target with a V flip).
    ComPtr<ID3D11VertexShader> blitVs;
    ComPtr<ID3D11PixelShader> blitPs;
    ComPtr<ID3D11SamplerState> blitSampler;
    ComPtr<ID3D11BlendState> blitBlend;
    ComPtr<ID3D11DepthStencilState> blitDepth;
    ComPtr<ID3D11RasterizerState> blitRaster;

    // Scissored clear fallback for feature level 10.x, where
    // ID3D11DeviceContext1::ClearView (the only rect-aware clear) is not
    // guaranteed to be available.  Draws a fullscreen triangle with a
    // constant color and SV_Depth, clipped by the scissor rectangle.
    ComPtr<ID3D11VertexShader> clearVs;
    ComPtr<ID3D11PixelShader> clearPs;
    ComPtr<ID3D11Buffer> clearConstants;
    ComPtr<ID3D11BlendState> clearBlend;
    ComPtr<ID3D11DepthStencilState> clearDepth;
    ComPtr<ID3D11RasterizerState> clearRaster;
};

// ---------------------------------------------------------------------------
// Device / swapchain.
// ---------------------------------------------------------------------------
Device* createDevice(HWND hwnd, std::string& error);
void destroyDevice(Device* device);

// Ensures the swapchain exists at the requested size and presents the back
// buffer.  `width`/`height` are framebuffer pixels.
bool present(Device* device, UINT width, UINT height, std::string& error);

// Blits a color texture to the current back buffer (with the final Y flip).
bool blitToBackBuffer(Device* device, Texture* source, std::string& error);

// Diagnostics: write mip 0 of a texture (or the swapchain back buffer) to a
// 32-bit BMP.  Rows are written bottom-up, matching the flipped render target
// layout (row 0 is the logical bottom).
bool dumpTexture(Device* device, Texture* texture, const std::string& path, std::string& error);
bool dumpBackBuffer(Device* device, const std::string& path, std::string& error);

// Human-readable swapchain state for the Java log.
std::string presentInfo(Device* device);

void setVsync(Device* device, bool vsync);

// ---------------------------------------------------------------------------
// Resource creation / destruction.
// ---------------------------------------------------------------------------
Texture* createTexture(
    Device* device,
    int usage,
    int format,
    UINT width,
    UINT height,
    UINT depthOrLayers,
    UINT mipLevels,
    std::string& error);

TextureView* createTextureView(
    Device* device,
    Texture* texture,
    UINT baseMip,
    UINT mipLevels,
    std::string& error);

void destroyTexture(Texture* texture);
void destroyTextureView(TextureView* view);

Buffer* createBuffer(
    Device* device,
    int usage,
    UINT size,
    const void* initialData,
    UINT initialSize,
    std::string& error);

void destroyBuffer(Buffer* buffer);

Sampler* createSampler(
    Device* device,
    int addressU,
    int addressV,
    int minFilter,
    int magFilter,
    int maxAnisotropy,
    bool hasMaxLod,
    double maxLod,
    std::string& error);

void destroySampler(Sampler* sampler);

// ---------------------------------------------------------------------------
// Commands.
// ---------------------------------------------------------------------------
bool writeToBuffer(
    Buffer* buffer,
    UINT offset,
    const void* data,
    UINT size,
    std::string& error);

// Maps a buffer slice.  Returns the CPU address of the first byte of the slice
// or nullptr on failure.  `read`/`write` follow GpuBuffer usage rules.
void* mapBuffer(
    Buffer* buffer,
    UINT offset,
    UINT size,
    bool read,
    bool write,
    std::string& error);

void unmapBuffer(Buffer* buffer);

bool copyBufferToBuffer(
    Buffer* source,
    UINT sourceOffset,
    Buffer* destination,
    UINT destinationOffset,
    UINT size,
    std::string& error);

bool writeToTexture(
    Texture* texture,
    UINT mip,
    UINT layer,
    UINT x,
    UINT y,
    UINT width,
    UINT height,
    const void* data,
    UINT srcRowStride,
    std::string& error);

bool copyTextureToBuffer(
    Texture* texture,
    Buffer* buffer,
    UINT bufferOffset,
    UINT mip,
    UINT x,
    UINT y,
    UINT width,
    UINT height,
    std::string& error);

bool copyTextureToTexture(
    Texture* source,
    Texture* destination,
    UINT mip,
    UINT srcX,
    UINT srcY,
    UINT dstX,
    UINT dstY,
    UINT width,
    UINT height,
    std::string& error);

bool clearColorTexture(
    Device* device,
    Texture* texture,
    UINT32 argb,
    std::string& error);

bool clearDepthTexture(
    Device* device,
    Texture* texture,
    double depth,
    std::string& error);

bool clearColorAndDepthTexture(
    Device* device,
    Texture* color,
    UINT32 argb,
    Texture* depth,
    double depthValue,
    const int* rect,
    std::string& error);

// ---------------------------------------------------------------------------
// Pipelines.
// ---------------------------------------------------------------------------
Pipeline* createPipeline(Device* device, const PipelineDesc& desc, std::string& error);
void destroyPipeline(Pipeline* pipeline);

// ---------------------------------------------------------------------------
// Render passes.
// ---------------------------------------------------------------------------
RenderPass* createRenderPass(
    Device* device,
    TextureView* colorView,
    TextureView* depthView,
    bool clearColor,
    UINT32 clearColorValue,
    bool clearDepth,
    double clearDepthValue,
    std::string& error);

void destroyRenderPass(RenderPass* pass);

void passSetPipeline(RenderPass* pass, Pipeline* pipeline);
void passSetUniform(RenderPass* pass, UINT slot, Buffer* buffer, UINT offset, UINT size);
// Binds an emulated typed buffer (D3D11 FL10.x has no Buffer<T>) as a texture.
// `format` uses the TextureFormat ordinal: 0=RGBA8, 1=RED8, 2=RED8I.
void passSetTexelBuffer(RenderPass* pass, UINT slot, Buffer* buffer, int format);
void passBindTexture(RenderPass* pass, UINT slot, TextureView* view, Sampler* sampler);
void passSetVertexBuffer(RenderPass* pass, Buffer* buffer);
void passSetIndexBuffer(RenderPass* pass, Buffer* buffer, bool index32);
void passSetScissor(RenderPass* pass, bool enabled, int x, int y, int width, int height);
void passDrawIndexed(RenderPass* pass, UINT firstIndex, UINT indexCount, int baseVertex, UINT instanceCount);
void passDraw(RenderPass* pass, UINT firstVertex, UINT vertexCount, UINT instanceCount);

// ---------------------------------------------------------------------------
// Fences / queries.
// ---------------------------------------------------------------------------
Fence* createFence(Device* device, std::string& error);
bool fenceAwait(Fence* fence, int64_t timeoutNanos);
void destroyFence(Fence* fence);

// Creates and begins a GPU timer query (timestamp disjoint).
Query* createTimerQuery(Device* device, std::string& error);
void timerQueryEnd(Query* query);

// Returns true and writes the elapsed GPU nanoseconds when the result is
// ready; returns false while the GPU has not finished the query yet.
bool queryGetValue(Query* query, int64_t& nanos, bool& ready);
void destroyQuery(Query* query);

}  // namespace dx11mc
