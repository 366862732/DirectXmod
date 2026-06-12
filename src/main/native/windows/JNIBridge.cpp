// JNIBridge.cpp - 完整版，无重复，适配 D3D12Renderer
#include <jni.h>
#include <windows.h>
#include <iostream>
#include "D3D12Renderer.h"

D3D12Renderer& getRenderer() {
    return D3D12Renderer::get();
}

#ifdef __cplusplus
extern "C" {
#endif

// ==================== 生命周期 ====================
JNIEXPORT jboolean JNICALL Java_com_dx12_client_D3D12Bridge_init
    (JNIEnv* env, jclass clazz, jlong hwnd, jint width, jint height) {
    return getRenderer().init((HWND)hwnd, width, height) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_resize
    (JNIEnv* env, jclass clazz, jint width, jint height) {
    getRenderer().resize(width, height);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_beginFrame
    (JNIEnv* env, jclass clazz) {
    getRenderer().beginFrame();
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_endFrame
    (JNIEnv* env, jclass clazz) {
    getRenderer().endFrame();
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_destroy
    (JNIEnv* env, jclass clazz) {
    getRenderer().shutdown();
}

// ==================== 缓冲区 ====================
JNIEXPORT jint JNICALL Java_com_dx12_client_D3D12Bridge_genBuffer
    (JNIEnv* env, jclass clazz) {
    return getRenderer().createBuffer();
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_deleteBuffer
    (JNIEnv* env, jclass clazz, jint id) {
    getRenderer().deleteBuffer(id);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_bindBuffer
    (JNIEnv* env, jclass clazz, jint target, jint id) {
    getRenderer().bindBuffer(target, id);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_bufferData
    (JNIEnv* env, jclass clazz, jint target, jobject data) {
    if (!data) return;
    void* ptr = env->GetDirectBufferAddress(data);
    jlong size = env->GetDirectBufferCapacity(data);
    // 简化：需要知道当前绑定的缓冲区 ID，这里先临时用全局状态（实际应由渲染器管理）
    // 为了演示，我们调用 uploadBufferData 时使用 0 作为 ID，你需要改成正确的 ID 获取方式
    // 更好的做法：在 bindBuffer 时记录当前缓冲区 ID，然后在这里使用它。
    int currentBuf = getRenderer().getCurrentVertexBuffer(); // 需要添加此方法
    getRenderer().uploadBufferData(currentBuf, ptr, (int)size);
}

// ==================== 纹理 ====================
JNIEXPORT jint JNICALL Java_com_dx12_client_D3D12Bridge_genTexture
    (JNIEnv* env, jclass clazz) {
    return getRenderer().createTexture();
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_deleteTexture
    (JNIEnv* env, jclass clazz, jint id) {
    getRenderer().deleteTexture(id);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_bindTexture
    (JNIEnv* env, jclass clazz, jint target, jint id) {
    getRenderer().bindTexture(target, id);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_texImage2D
    (JNIEnv* env, jclass clazz, jint target, jint level, jint internalFormat,
     jint width, jint height, jint border, jint format, jint type, jobject pixels) {
    void* data = pixels ? env->GetDirectBufferAddress(pixels) : nullptr;
    jlong size = pixels ? env->GetDirectBufferCapacity(pixels) : 0;
    std::cout << "[DX12] texImage2D: " << width << "x" << height << ", size=" << size << std::endl;
    int currentTex = getRenderer().getCurrentTexture();
    getRenderer().uploadTextureData(currentTex, width, height, data);
}

// ==================== 绘制 ====================
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_drawElements
    (JNIEnv* env, jclass clazz, jint mode, jint count, jint type, jlong indices) {
    getRenderer().drawElements(mode, count, type, indices);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_drawArrays
    (JNIEnv* env, jclass clazz, jint mode, jint first, jint count) {
    getRenderer().drawArrays(mode, first, count);
}

// ==================== 状态 ====================
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_viewport
    (JNIEnv* env, jclass clazz, jint x, jint y, jint width, jint height) {
    getRenderer().setViewport(x, y, width, height);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_clearColor
    (JNIEnv* env, jclass clazz, jfloat r, jfloat g, jfloat b, jfloat a) {
    getRenderer().setClearColor(r, g, b, a);
}

JNIEXPORT jlong JNICALL Java_com_dx12_client_D3D12Bridge_getBufferAddress
    (JNIEnv* env, jclass clazz, jobject buffer) {
    if (!buffer) return 0;
    return (jlong)env->GetDirectBufferAddress(buffer);
}

// ==================== 其他空实现（避免链接错误）====================
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_enableDepthTest(JNIEnv* e, jclass c, jboolean en) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_depthFunc(JNIEnv* e, jclass c, jint func) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_enableBlend(JNIEnv* e, jclass c, jboolean en) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_blendFunc(JNIEnv* e, jclass c, jint src, jint dst) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_enableCullFace(JNIEnv* e, jclass c, jboolean en) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_cullFace(JNIEnv* e, jclass c, jint mode) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_clear(JNIEnv* e, jclass c, jint mask) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_uniform1f(JNIEnv* e, jclass c, jint loc, jfloat v) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_uniform2f(JNIEnv* e, jclass c, jint loc, jfloat v0, jfloat v1) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_uniform3f(JNIEnv* e, jclass c, jint loc, jfloat v0, jfloat v1, jfloat v2) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_uniform4f(JNIEnv* e, jclass c, jint loc, jfloat v0, jfloat v1, jfloat v2, jfloat v3) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_uniformMatrix4fv(JNIEnv* e, jclass c, jint loc, jboolean transpose, jfloatArray val) {}
JNIEXPORT jint JNICALL Java_com_dx12_client_D3D12Bridge_getUniformLocation(JNIEnv* e, jclass c, jint prog, jstring name) { return 0; }
JNIEXPORT jint JNICALL Java_com_dx12_client_D3D12Bridge_getShaderiv(JNIEnv* e, jclass c, jint shader, jint pname) { return 1; }
JNIEXPORT jint JNICALL Java_com_dx12_client_D3D12Bridge_getProgramiv(JNIEnv* e, jclass c, jint program, jint pname) { return 1; }
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_texParameteri(JNIEnv* e, jclass c, jint target, jint pname, jint param) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_texParameterf(JNIEnv* e, jclass c, jint target, jint pname, jfloat param) {}
JNIEXPORT jint JNICALL Java_com_dx12_client_D3D12Bridge_createProgram(JNIEnv* e, jclass c) { return getRenderer().createProgram(); }
JNIEXPORT jint JNICALL Java_com_dx12_client_D3D12Bridge_createShader(JNIEnv* e, jclass c, jint type) { return getRenderer().createShader(type); }
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_shaderSource(JNIEnv* e, jclass c, jint shader, jstring source) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_compileShader(JNIEnv* e, jclass c, jint shader) {}
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_attachShader(JNIEnv* e, jclass c, jint program, jint shader) { getRenderer().attachShader(program, shader); }
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_linkProgram(JNIEnv* e, jclass c, jint program) { getRenderer().linkProgram(program); }
JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_useProgram(JNIEnv* e, jclass c, jint program) { getRenderer().useProgram(program); }

#ifdef __cplusplus
}
#endif