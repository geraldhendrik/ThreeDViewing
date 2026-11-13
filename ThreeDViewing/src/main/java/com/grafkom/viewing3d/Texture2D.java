package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * Memuat gambar PNG/JPG (dari classpath / folder resources) memakai STBImage dan
 * mengunggahnya ke GPU sebagai tekstur 2D lengkap dengan mipmap.
 */
public class Texture2D {

    private final int textureID;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // stb memuat baris pertama gambar di atas, sedangkan OpenGL menganggap v=0 di bawah
        stbi_set_flip_vertically_on_load(true);

        ByteBuffer raw = null;
        ByteBuffer image = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            raw = readToDirectBuffer(resourcePath);
            if (raw != null) {
                image = stbi_load_from_memory(raw, width, height, channels, 4);
            }

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            if (image != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, image);
            } else {
                System.err.println("Failed to load texture: " + resourcePath + " -> pakai tekstur pengganti");
                uploadFallback();
            }
        } finally {
            if (image != null) stbi_image_free(image);
            if (raw != null) MemoryUtil.memFree(raw);
        }

        glGenerateMipmap(GL_TEXTURE_2D);
        setFilter(true);
    }

    /**
     * true  = Bilinear + Mipmapped (GL_LINEAR_MIPMAP_LINEAR / GL_LINEAR)
     * false = Nearest Neighbor     (GL_NEAREST)
     */
    public void setFilter(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }

    // ------------------------------------------------------------------ helper
    private static ByteBuffer readToDirectBuffer(String path) {
        try (InputStream in = Texture2D.class.getResourceAsStream(path)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes);
            buf.flip();
            return buf;
        } catch (IOException e) {
            return null;
        }
    }

    /** Papan catur magenta/hitam 64x64 sebagai tanda bahwa file tekstur gagal dimuat. */
    private static void uploadFallback() {
        final int size = 64;
        ByteBuffer buf = MemoryUtil.memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean on = ((x / 8) + (y / 8)) % 2 == 0;
                buf.put((byte) (on ? 255 : 0)).put((byte) 0).put((byte) (on ? 255 : 0)).put((byte) 255);
            }
        }
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        MemoryUtil.memFree(buf);
    }
}
