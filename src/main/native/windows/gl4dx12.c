#include <jni.h>
#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// ?? OpenGL ??
typedef struct {
    int id;
    int type;
    int width, height;
    int size;
} GLObject;

static GLObject g_textures[1024];
static GLObject g_buffers[1024];
static int g_nextTextureId = 1;
static int g_nextBufferId = 1;
static int g_currentTexture2D = 0;
static int g_currentArrayBuffer = 0;
static int g_currentElementArrayBuffer = 0;

// ???
void initObjects() {
    memset(g_textures, 0, sizeof(g_textures));
    memset(g_buffers, 0, sizeof(g_buffers));
}

// ????
int addTexture() {
    for (int i = 0; i < 1024; i++) {
        if (g_textures[i].id == 0) {
            g_textures[i].id = g_nextTextureId++;
            g_textures[i].type = 0;
            return g_textures[i].id;
        }
    }
    return 0;
}

// ?????
int addBuffer() {
    for (int i = 0; i < 1024; i++) {
        if (g_buffers[i].id == 0) {
            g_buffers[i].id = g_nextBufferId++;
            g_buffers[i].type = 1;
            return g_buffers[i].id;
        }
    }
    return 0;
}

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    if (fdwReason == DLL_PROCESS_ATTACH) {
        initObjects();
        OutputDebugStringA("[GL4DX12] DLL loaded\n");
    }
    return TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_client_DX12LibClient_nativeInit
    (JNIEnv* env, jclass clazz, jlong hwnd, jint width, jint height) {
    OutputDebugStringA("[GL4DX12] nativeInit called\n");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeResize(JNIEnv* e, jclass c, jint w, jint h) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeRender(JNIEnv* e, jclass c) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativePresent(JNIEnv* e, jclass c) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeDestroy(JNIEnv* e, jclass c) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeSetEnabled(JNIEnv* e, jclass c, jboolean en) {}
JNIEXPORT jlong JNICALL Java_com_dx12_client_DX12LibClient_getBufferAddress(JNIEnv* e, jclass c, jobject buf) { return 0; }

// ========== OpenGL ?? ==========

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindTexture
    (JNIEnv* e, jclass c, jint target, jint texture) {
    if (target == 0x0DE1) g_currentTexture2D = texture;
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenTextures
    (JNIEnv* e, jclass c, jint n, jlong textures) {
    jint* arr = (jint*)textures;
    for (int i = 0; i < n; i++) {
        arr[i] = addTexture();
        char buf[64];
        snprintf(buf, sizeof(buf), "[GL4DX12] glGenTextures: %d\n", arr[i]);
        OutputDebugStringA(buf);
    }
}

// ??????????
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenTexturesNative
    (JNIEnv* e, jclass c, jint n, jlong textures) {
    Java_com_dx12_client_DX12LibClient_glGenTextures(e, c, n, textures);
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDeleteTextures
    (JNIEnv* e, jclass c, jint n, jlong textures) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glTexImage2D
    (JNIEnv* e, jclass c, jint target, jint level, jint internalFormat,
     jint width, jint height, jint border, jint format, jint type, jlong pixels) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindBuffer
    (JNIEnv* e, jclass c, jint target, jint buffer) {
    if (target == 0x8892) g_currentArrayBuffer = buffer;
    if (target == 0x8893) g_currentElementArrayBuffer = buffer;
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenBuffers
    (JNIEnv* e, jclass c, jint n, jlong buffers) {
    jint* arr = (jint*)buffers;
    for (int i = 0; i < n; i++) {
        arr[i] = addBuffer();
        char buf[64];
        snprintf(buf, sizeof(buf), "[GL4DX12] glGenBuffers: %d\n", arr[i]);
        OutputDebugStringA(buf);
    }
}

// ??????????
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenBuffersNative
    (JNIEnv* e, jclass c, jint n, jlong buffers) {
    Java_com_dx12_client_DX12LibClient_glGenBuffers(e, c, n, buffers);
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBufferData
    (JNIEnv* e, jclass c, jint target, jlong size, jlong data, jint usage) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUseProgram
    (JNIEnv* e, jclass c, jint program) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawElements
    (JNIEnv* e, jclass c, jint mode, jint count, jint type, jlong indices) {
    char buf[256];
    snprintf(buf, sizeof(buf), "[GL4DX12] glDrawElements: mode=%d, count=%d, tex=%d\n", mode, count, g_currentTexture2D);
    OutputDebugStringA(buf);
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawArrays
    (JNIEnv* e, jclass c, jint mode, jint first, jint count) {}

JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateProgram
    (JNIEnv* e, jclass c) { static int id=100; return ++id; }

JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateShader
    (JNIEnv* e, jclass c, jint type) { static int id=200; return ++id; }

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glShaderSource
    (JNIEnv* e, jclass c, jint shader, jint count, jlong strings, jlong lengths) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glCompileShader
    (JNIEnv* e, jclass c, jint shader) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glAttachShader
    (JNIEnv* e, jclass c, jint program, jint shader) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glLinkProgram
    (JNIEnv* e, jclass c, jint program) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGetShaderiv
    (JNIEnv* e, jclass c, jint shader, jint pname, jlong params) {
    if (pname == 0x8B81) { int* p = (int*)params; *p = 1; }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGetProgramiv
    (JNIEnv* e, jclass c, jint program, jint pname, jlong params) {
    if (pname == 0x8B82) { int* p = (int*)params; *p = 1; }
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniform1f
    (JNIEnv* e, jclass c, jint location, jfloat v0) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniformMatrix4fv
    (JNIEnv* e, jclass c, jint location, jint count, jboolean transpose, jlong value) {}

JNIEXPORT jlong JNICALL Java_com_dx12_client_DX12LibClient_getBufferAddressNative
    (JNIEnv* e, jclass c, jobject buffer) { return 0; }
