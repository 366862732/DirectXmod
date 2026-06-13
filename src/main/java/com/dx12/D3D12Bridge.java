// 加回缺失import
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import com.dx12.render.SkyboxExtractor;
import com.dx12.render.ParticleExtractor;
private static LevelRenderState cachedLevelState;
private static SkyRenderState cachedSkyState;

// 新增方法：渲染完整帧
public static void renderFullFrame(LevelRenderState levelState, CameraRenderState cameraState, float partialTick) {
    if (!d3d12Ready || !d3d12Active) return;

    cachedLevelState = levelState;

    // 1. 同步矩阵
    syncMatrices();

    // 2. 渲染天空盒
    if (levelState.skyRenderState != null) {
        renderSkybox(levelState.skyRenderState);
    }

    // 3. 渲染粒子 (在方块之后，叠加效果)
    if (levelState.particlesRenderState != null) {
        renderParticles(levelState.particlesRenderState);
    }

    // 4. 提交并 Present
    nativePresent();
}

private static void renderSkybox(SkyRenderState skyState) {
    float[] data = SkyboxExtractor.extractSkyData(skyState);
    if (data != null) {
        nativeSetSkyParameters(data);
        nativeRenderSkybox();
    }
}

private static void renderParticles(ParticlesRenderState particlesState) {
    List<float[]> particles = ParticleExtractor.extractParticles(particlesState);
    if (!particles.isEmpty()) {
        // 批量上传粒子数据到 D3D12
        float[] flatArray = new float[particles.size() * 14];
        int idx = 0;
        for (float[] p : particles) {
            System.arraycopy(p, 0, flatArray, idx, 14);
            idx += 14;
        }
        nativeUploadParticles(flatArray, particles.size());
        nativeRenderParticles();
    }
}

// Native 方法声明（添加到 DX12LibClient）
public static native void nativeSetSkyParameters(float[] params);
public static native void nativeRenderSkybox();
public static native void nativeUploadParticles(float[] particles, int count);
public static native void nativeRenderParticles();