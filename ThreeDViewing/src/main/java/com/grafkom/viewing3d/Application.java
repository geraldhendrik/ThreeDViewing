package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite.
 *
 * Kontrol:
 *   B  : Phong  <-> Blinn-Phong            (Indikator 1)
 *   F  : Flat   <-> Smooth shading         (Indikator 2)
 *   Z  : Z-Buffer (depth test) ON/OFF      (Indikator 3)
 *   C  : Back-Face Culling ON/OFF          (Indikator 3)
 *   T  : Filter tekstur Nearest <-> Bilinear+Mipmap (Indikator 4)
 *   X  : Tekstur ON/OFF (warna polos)      (Indikator 4)
 *   S  : Shadow Mapping ON/OFF             (Indikator 5)
 *   N / M : kurangi / tambah Shadow Bias   (Indikator 5)
 *   1 / 2 / 3 : Sphere / Torus / Rumah 3D
 *   L  : animasi cahaya ON/OFF      R : rotasi model ON/OFF
 *   V  : VSync ON/OFF (matikan untuk mengukur FPS)
 *   Mouse drag / tombol panah : orbit kamera,  scroll : zoom
 *   H  : tampilkan bantuan,  ESC : keluar
 */
public class Application {

    private static final String TITLE = "3D Visual Realism Suite";
    private static final float GROUND_Y = -1.5f;

    private long window;
    private int fbWidth = 1280, fbHeight = 720;

    // GPU resources
    private Shader sceneShader, depthShader;
    private Mesh[] models;
    private String[] modelNames;
    private Vector3f[] modelColors;
    private Mesh ground;
    private Texture2D brickTexture, checkerTexture;
    private ShadowMapFBO shadowFBO;

    private final Camera camera = new Camera();

    // state yang dikontrol keyboard
    private boolean blinnPhong = true;
    private boolean smooth = true;
    private boolean depthTest = true;
    private boolean backFaceCulling = true;
    private boolean mipmapped = true;
    private boolean useTexture = true;
    private boolean shadows = true;
    private boolean animateLight = false;
    private boolean rotateModel = false;
    private boolean vsync = false;
    private int modelIndex = 0;

    private float shadowBiasScale = 0.005f;
    private static final float SHADOW_BIAS_MIN = 0.0005f;

    private float lightAngle = 2.35f; // posisi awal cahaya sekitar (-4, 7, 4)
    private float modelAngle = 0f;

    // mouse
    private double lastX, lastY;
    private boolean dragging = false;

    // FPS
    private double fps = 0;

    // ------------------------------------------------------------------ siklus hidup
    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4); // MSAA 4x

        window = glfwCreateWindow(1280, 720, TITLE, NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Gagal membuat window (butuh OpenGL 3.3)");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(vsync ? 1 : 0);
        GL.createCapabilities();

        setupCallbacks();

        // --- resource GPU
        sceneShader = new Shader("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new Shader("/shaders/depth.vert", "/shaders/depth.frag");

        models = new Mesh[]{
            new Mesh(MeshFactory.sphere(1.2f, 16, 24)),
            new Mesh(MeshFactory.torus(1.2f, 0.5f, 24, 16)),
            new Mesh(MeshFactory.house())
        };
        modelNames = new String[]{"Sphere", "Torus", "Rumah 3D"};
        modelColors = new Vector3f[]{
            new Vector3f(0.90f, 0.40f, 0.30f),
            new Vector3f(0.30f, 0.70f, 0.90f),
            new Vector3f(0.85f, 0.75f, 0.55f)
        };
        ground = new Mesh(MeshFactory.plane(20f, 10f));

        brickTexture = new Texture2D("/textures/brick.png");
        checkerTexture = new Texture2D("/textures/checker.png");
        brickTexture.setFilter(mipmapped);
        checkerTexture.setFilter(mipmapped);

        shadowFBO = new ShadowMapFBO();

        glEnable(GL_MULTISAMPLE);
        glClearColor(0.53f, 0.70f, 0.85f, 1f);

        glfwShowWindow(window);
        printHelp();
        updateTitle();
    }

    private void setupCallbacks() {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_RELEASE) return;
            boolean press = action == GLFW_PRESS;

            switch (key) {
                case GLFW_KEY_ESCAPE -> { if (press) glfwSetWindowShouldClose(win, true); }
                case GLFW_KEY_H -> { if (press) printHelp(); }
                case GLFW_KEY_B -> { if (press) { blinnPhong = !blinnPhong; log("Shading model: " + (blinnPhong ? "Blinn-Phong" : "Phong")); } }
                case GLFW_KEY_F -> { if (press) { smooth = !smooth; log("Shading: " + (smooth ? "Smooth (vertex normal averaging)" : "Flat (per-face normal)")); } }
                case GLFW_KEY_Z -> { if (press) { depthTest = !depthTest; log("Z-Buffer depth test: " + onOff(depthTest)); } }
                case GLFW_KEY_C -> { if (press) { backFaceCulling = !backFaceCulling; log("Back-face culling: " + onOff(backFaceCulling)); } }
                case GLFW_KEY_T -> {
                    if (press) {
                        mipmapped = !mipmapped;
                        brickTexture.setFilter(mipmapped);
                        checkerTexture.setFilter(mipmapped);
                        log("Texture filter: " + (mipmapped ? "Bilinear + Mipmap" : "Nearest"));
                    }
                }
                case GLFW_KEY_X -> { if (press) { useTexture = !useTexture; log("Texture: " + onOff(useTexture)); } }
                case GLFW_KEY_S -> { if (press) { shadows = !shadows; log("Shadow mapping: " + onOff(shadows)); } }
                case GLFW_KEY_N -> { shadowBiasScale = Math.max(0f, shadowBiasScale / 1.5f); log(String.format("Shadow bias (max) = %.5f", shadowBiasScale)); }
                case GLFW_KEY_M -> { shadowBiasScale = Math.min(0.2f, Math.max(0.0001f, shadowBiasScale) * 1.5f); log(String.format("Shadow bias (max) = %.5f", shadowBiasScale)); }
                case GLFW_KEY_L -> { if (press) { animateLight = !animateLight; log("Animasi cahaya: " + onOff(animateLight)); } }
                case GLFW_KEY_R -> { if (press) { rotateModel = !rotateModel; log("Rotasi model: " + onOff(rotateModel)); } }
                case GLFW_KEY_V -> { if (press) { vsync = !vsync; glfwSwapInterval(vsync ? 1 : 0); log("VSync: " + onOff(vsync)); } }
                case GLFW_KEY_1 -> { if (press) selectModel(0); }
                case GLFW_KEY_2 -> { if (press) selectModel(1); }
                case GLFW_KEY_3 -> { if (press) selectModel(2); }
                default -> { }
            }
            updateTitle();
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) camera.rotate((float) (-(x - lastX) * 0.4), (float) ((y - lastY) * 0.4));
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (win, dx, dy) -> camera.zoom((float) dy * 0.8f));
    }

    private void selectModel(int i) {
        modelIndex = i;
        log("Model: " + modelNames[i] + " (" + models[i].getTriangleCount() + " segitiga)");
    }

    // ------------------------------------------------------------------ loop
    private void loop() {
        double last = glfwGetTime();
        double fpsTimer = 0;
        int frames = 0;

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            glfwPollEvents();
            handleHeldKeys(dt);
            update(dt);
            render();
            glfwSwapBuffers(window);

            frames++;
            fpsTimer += dt;
            if (fpsTimer >= 0.5) {
                fps = frames / fpsTimer;
                frames = 0;
                fpsTimer = 0;
                updateTitle();
            }
        }
    }

    private void handleHeldKeys(float dt) {
        float speed = 90f * dt;
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) camera.rotate(-speed, 0);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.rotate(speed, 0);
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) camera.rotate(0, speed);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) camera.rotate(0, -speed);
    }

    private void update(float dt) {
        if (animateLight) lightAngle += dt * 0.6f;
        if (rotateModel) modelAngle += dt * 0.8f;
    }

    // ------------------------------------------------------------------ render
    private void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }
        if (fbWidth == 0 || fbHeight == 0) return; // jendela diminimize

        Vector3f lightPos = new Vector3f(
                5.7f * (float) Math.cos(lightAngle), 7f, 5.7f * (float) Math.sin(lightAngle));

        // Matriks ruang cahaya: proyeksi ortografik + view dari posisi cahaya ke pusat scene
        Matrix4f lightSpace = new Matrix4f()
                .ortho(-8f, 8f, -8f, 8f, 1f, 30f)
                .lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));

        // ============ PASS 1: Depth Pass (dari sudut pandang cahaya) ============
        if (shadows) {
            shadowFBO.bindForWriting();
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            glDisable(GL_CULL_FACE);

            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            depthShader.setMat4("model", modelMatrix());
            models[modelIndex].draw(smooth); // lantai tidak perlu masuk shadow map

            shadowFBO.unbind(fbWidth, fbHeight);
        }

        // ============ PASS 2: Lighting Pass (dari sudut pandang kamera) ============
        glViewport(0, 0, fbWidth, fbHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(backFaceCulling);

        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(45.0), (float) fbWidth / fbHeight, 0.5f, 100f);

        sceneShader.use();
        sceneShader.setMat4("view", camera.getViewMatrix());
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightColor", new Vector3f(1f, 1f, 1f));
        sceneShader.setBool("useBlinnPhong", blinnPhong);
        sceneShader.setBool("useShadow", shadows);
        sceneShader.setFloat("shadowBiasScale", shadowBiasScale);
        sceneShader.setFloat("shadowBiasMin", SHADOW_BIAS_MIN);
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 2);

        shadowFBO.bindDepthTexture(2);

        // lantai digambar lebih dulu, lalu model
        sceneShader.setMat4("model", new Matrix4f().translate(0, GROUND_Y, 0));
        sceneShader.setVec3("objectColor", new Vector3f(0.55f, 0.60f, 0.70f));
        sceneShader.setFloat("specularStrength", 0.15f);
        sceneShader.setBool("useTexture", useTexture);
        checkerTexture.bind(0);
        ground.draw(true);

        sceneShader.setMat4("model", modelMatrix());
        sceneShader.setVec3("objectColor", modelColors[modelIndex]);
        sceneShader.setFloat("specularStrength", 0.8f);
        sceneShader.setBool("useTexture", useTexture);
        brickTexture.bind(0);
        models[modelIndex].draw(smooth);
    }

    private Matrix4f modelMatrix() {
        return new Matrix4f().rotateY(modelAngle);
    }

    // ------------------------------------------------------------------ util
    private void updateTitle() {
        String t = String.format(
                "%s | %s | FPS %.0f | %s | %s | Z:%s Cull:%s | Tex:%s (%s) | Shadow:%s bias=%.4f",
                TITLE, modelNames == null ? "-" : modelNames[modelIndex], fps,
                blinnPhong ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                onOff(depthTest), onOff(backFaceCulling),
                onOff(useTexture), mipmapped ? "Mipmap" : "Nearest",
                onOff(shadows), shadowBiasScale);
        glfwSetWindowTitle(window, t);
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    private static void log(String s) {
        System.out.println("[UI] " + s);
    }

    private static void printHelp() {
        System.out.println("""
                ==== 3D Visual Realism Suite - Kontrol ====
                 B  Phong <-> Blinn-Phong        F  Flat <-> Smooth
                 Z  Z-Buffer ON/OFF              C  Back-Face Culling ON/OFF
                 T  Filter Nearest <-> Mipmap    X  Tekstur ON/OFF
                 S  Shadow Mapping ON/OFF        N/M  Shadow bias - / +
                 1/2/3  Sphere / Torus / Rumah   L  animasi cahaya    R  rotasi model
                 V  VSync ON/OFF (matikan utk ukur FPS)
                 Mouse drag / panah = orbit,  scroll = zoom,  H = bantuan,  ESC = keluar
                ============================================""");
    }

    private void cleanup() {
        if (window != NULL) {
            if (models != null) for (Mesh m : models) m.cleanup();
            if (ground != null) ground.cleanup();
            if (sceneShader != null) sceneShader.cleanup();
            if (depthShader != null) depthShader.cleanup();
            if (brickTexture != null) brickTexture.cleanup();
            if (checkerTexture != null) checkerTexture.cleanup();
            if (shadowFBO != null) shadowFBO.cleanup();
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
