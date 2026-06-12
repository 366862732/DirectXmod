#include <jni.h>
#include <windows.h>
#include <stdio.h>
#include <stdlib.h>

// ?? OpenGL ??
typedef struct {
    int id;
    int type;  // 0=texture, 1=buffer
    int width, height;
    int size;
} GLObject;

// ????
#define MAX_OBJECTS 1024
static GLObject g_textures[MAX_OBJECTS];
static GLObject g_buffers[MAX_OBJECTS];
static int g_nextTextureId = 1;
static int g_nextBufferId = 1;

// ??????
static int g_currentTexture2D = 0;
static int g_currentArrayBuffer = 0;
static int g_currentElementArrayBuffer = 0;

// ????
GLObject* findTexture(int id) {
    for (int i = 0; i < MAX_OBJECTS; i++) {
        if (g_textures[i].id == id && g_textures[i].type == 0) {
            return &g_textures[i];
        }
    }
    return NULL;
}

// ?????
GLObject* findBuffer(int id) {
    for (int i = 0; i < MAX_OBJECTS; i++) {
        if (g_buffers[i].id == id && g_buffers[i].type == 1) {
            return &g_buffers[i];
        }
    }
    return NULL;
}

// ????
int addTexture() {
    for (int i = 0; i < MAX_OBJECTS; i++) {
        if (g_textures[i].id == 0) {
            g_textures[i].id = g_nextTextureId++;
            g_textures[i].type = 0;
            g_textures[i].width = 0;
            g_textures[i].height = 0;
            return g_textures[i].id;
        }
    }
    return 0;
}

// ????
void deleteTexture(int id) {
    for (int i = 0; i < MAX_OBJECTS; i++) {
        if (g_textures[i].id == id) {
            g_textures[i].id = 0;
            break;
        }
    }
}

// ?????
int addBuffer() {
    for (int i = 0; i < MAX_OBJECTS; i++) {
        if (g_buffers[i].id == 0) {
            g_buffers[i].id = g_nextBufferId++;
            g_buffers[i].type = 1;
            g_buffers[i].size = 0;
            return g_buffers[i].id;
        }
    }
    return 0;
}

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    if (fdwReason == DLL_PROCESS_ATTACH) {
        // ???????
        memset(g_textures, 0, sizeof(g_textures));
        memset(g_buffers, 0, sizeof(g_buffers));
        OutputDebugStringA("[GL4DX12] DLL loaded\n");
    }
    return TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_client_DX12LibClient_nativeInit
    (JNIEnv* env, jclass clazz, jlong hwnd, jint width, jint height) {
    OutputDebugStringA("[GL4DX12] nativeInit called\n");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeResize
    (JNIEnv* env, jclass clazz, jint width, jint height) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeRender
    (JNIEnv* env, jclass clazz) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativePresent
    (JNIEnv* env, jclass clazz) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeDestroy
    (JNIEnv* env, jclass clazz) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeSetEnabled
    (JNIEnv* env, jclass clazz, jboolean enabled) {}

JNIEXPORT jlong JNICALL Java_com_dx12_client_DX12LibClient_getBufferAddress
    (JNIEnv* env, jclass clazz, jobject buffer) {
    return 0;
}

// ========== OpenGL ???? ==========

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindTexture
    (JNIEnv* env, jclass clazz, jint target, jint texture) {
    if (target == 0x0DE1) {  // GL_TEXTURE_2D
        g_currentTexture2D = texture;
    }
    char buf[128];
    snprintf(buf, sizeof(buf), "[GL4DX12] glBindTexture: target=%d, tex=%d\n", target, texture);
    OutputDebugStringA(buf);
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenTextures
    (JNIEnv* env, jclass clazz, jint n, jlong textures) {
    jint* arr = (jint*)textures;
    for (int i = 0; i < n; i++) {
        arr[i] = addTexture();
        char buf[64];
        snprintf(buf, sizeof(buf), "[GL4DX12] glGenTextures: %d\n", arr[i]);
        OutputDebugStringA(buf);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDeleteTextures
    (JNIEnv* env, jclass clazz, jint n, jlong textures) {
    jint* arr = (jint*)textures;
    for (int i = 0; i < n; i++) {
        deleteTexture(arr[i]);
        char buf[64];
        snprintf(buf, sizeof(buf), "[GL4DX12] glDeleteTextures: %d\n", arr[i]);
        OutputDebugStringA(buf);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glTexImage2D
    (JNIEnv* env, jclass clazz, jint target, jint level, jint internalFormat,
     jint width, jint height, jint border, jint format, jint type, jlong pixels) {
    GLObject* tex = findTexture(g_currentTexture2D);
    if (tex) {
        tex->width = width;
        tex->height = height;
        char buf[128];
        snprintf(buf, sizeof(buf), "[GL4DX12] glTexImage2D: tex=%d, %dx%d\n", 
                 g_currentTexture2D, width, height);
        OutputDebugStringA(buf);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindBuffer
    (JNIEnv* env, jclass clazz, jint target, jint buffer) {
    if (target == 0x8892) {  // GL_ARRAY_BUFFER
        g_currentArrayBuffer = buffer;
    } else if (target == 0x8893) {  // GL_ELEMENT_ARRAY_BUFFER
        g_currentElementArrayBuffer = buffer;
    }
    char buf[128];
    snprintf(buf, sizeof(buf), "[GL4DX12] glBindBuffer: target=%d, buf=%d\n", target, buffer);
    OutputDebugStringA(buf);
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenBuffers
    (JNIEnv* env, jclass clazz, jint n, jlong buffers) {
    jint* arr = (jint*)buffers;
    for (int i = 0; i < n; i++) {
        arr[i] = addBuffer();
        char buf[64];
        snprintf(buf, sizeof(buf), "[GL4DX12] glGenBuffers: %d\n", arr[i]);
        OutputDebugStringA(buf);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBufferData
    (JNIEnv* env, jclass clazz, jint target, jlong size, jlong data, jint usage) {
    int currentBuf = (target == 0x8892) ? g_currentArrayBuffer : g_currentElementArrayBuffer;
    GLObject* buf = findBuffer(currentBuf);
    if (buf) {
        buf->size = (int)size;
        char bufstr[128];
        snprintf(bufstr, sizeof(bufstr), "[GL4DX12] glBufferData: buf=%d, size=%lld\n", 
                 currentBuf, (long long)size);
        OutputDebugStringA(bufstr);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUseProgram
    (JNIEnv* env, jclass clazz, jint program) {
    char buf[64];
    snprintf(buf, sizeof(buf), "[GL4DX12] glUseProgram: %d\n", program);
    OutputDebugStringA(buf);
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawElements
    (JNIEnv* env, jclass clazz, jint mode, jint count, jint type, jlong indices) {
    char buf[256];
    snprintf(buf, sizeof(buf), 
             "[GL4DX12] glDrawElements: mode=%d, count=%d, tex=%d, vbo=%d, ibo=%d\n",
             mode, count, g_currentTexture2D, g_currentArrayBuffer, g_currentElementArrayBuffer);
    OutputDebugStringA(buf);
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawArrays
    (JNIEnv* env, jclass clazz, jint mode, jint first, jint count) {
    char buf[256];
    snprintf(buf, sizeof(buf), 
             "[GL4DX12] glDrawArrays: mode=%d, first=%d, count=%d, tex=%d, vbo=%d\n",
             mode, first, count, g_currentTexture2D, g_currentArrayBuffer);
    OutputDebugStringA(buf);
}

// ?????? ID
JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateProgram
    (JNIEnv* env, jclass clazz) {
    static int nextId = 100;
    int id = ++nextId;
    char buf[64];
    snprintf(buf, sizeof(buf), "[GL4DX12] glCreateProgram -> %d\n", id);
    OutputDebugStringA(buf);
    return id;
}

JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateShader
    (JNIEnv* env, jclass clazz, jint type) {
    static int nextId = 200;
    int id = ++nextId;
    char buf[64];
    snprintf(buf, sizeof(buf), "[GL4DX12] glCreateShader -> %d\n", id);
    OutputDebugStringA(buf);
    return id;
}

// ????
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glShaderSource
    (JNIEnv* env, jclass clazz, jint shader, jint count, jlong strings, jlong lengths) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glCompileShader
    (JNIEnv* env, jclass clazz, jint shader) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glAttachShader
    (JNIEnv* env, jclass clazz, jint program, jint shader) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glLinkProgram
    (JNIEnv* env, jclass clazz, jint program) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGetShaderiv
    (JNIEnv* env, jclass clazz, jint shader, jint pname, jlong params) {
    if (pname == 0x8B81) {  // GL_COMPILE_STATUS
        int* p = (int*)params;
        *p = 1;  // ????
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGetProgramiv
    (JNIEnv* env, jclass clazz, jint program, jint pname, jlong params) {
    if (pname == 0x8B82) {  // GL_LINK_STATUS
        int* p = (int*)params;
        *p = 1;  // ????
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniform1f
    (JNIEnv* env, jclass clazz, jint location, jfloat v0) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniformMatrix4fv
    (JNIEnv* env, jclass clazz, jint location, jint count, jboolean transpose, jlong value) {}
