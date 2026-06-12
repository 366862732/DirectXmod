#pragma once

#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <unordered_map>
#include <iostream>

struct GLDrawState {
    int currentTexture2D = 0;
    int currentTextureCubeMap = 0;
    int currentArrayBuffer = 0;
    int currentElementArrayBuffer = 0;
    int currentProgram = 0;
    bool blendEnabled = false;
    bool depthTestEnabled = true;
    int viewportX = 0, viewportY = 0;
    int viewportWidth = 0, viewportHeight = 0;
};

struct TextureResource {
    int width = 0, height = 0;
    ID3D12Resource* resource = nullptr;
};

struct BufferResource {
    int size = 0;
    ID3D12Resource* resource = nullptr;
};

class OpenGLState {
public:
    static OpenGLState& get() {
        static OpenGLState instance;
        return instance;
    }
    
    void setTexture(int target, int texture);
    void genTexture(int texture);
    void setBuffer(int target, int buffer);
    void genBuffer(int buffer);
    void setProgram(int program);
    int createProgram();
    int createShader(int type);
    GLDrawState& getState() { return m_state; }
    
private:
    OpenGLState() = default;
    
    GLDrawState m_state;
    std::unordered_map<int, TextureResource> m_textures;
    std::unordered_map<int, BufferResource> m_buffers;
    int m_nextTextureId = 1;
    int m_nextBufferId = 1;
    int m_nextProgramId = 1;
    int m_nextShaderId = 1;
};

extern ID3D12Device* g_device;
extern ID3D12CommandQueue* g_queue;
extern IDXGISwapChain3* g_swapChain;
extern bool g_enabled;
extern bool g_enabled; 
