package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL33.*;

/**
 * Mesh di GPU. Menyimpan DUA VAO:
 *  - vaoSmooth : atribut normal = vertex normal averaging  (Smooth Shading)
 *  - vaoFlat   : atribut normal = normal per face          (Flat Shading)
 * Beralih Flat/Smooth cukup memilih VAO mana yang digambar.
 *
 * Layout atribut (sama dengan phong.vert):
 *   location 0 = aPos (vec3), 1 = aNormal (vec3), 2 = aTexCoords (vec2)
 */
public class Mesh {

    private final int vertexCount;
    private final int vaoSmooth, vaoFlat;
    private final int vboPos, vboSmooth, vboFlat, vboUV;

    public Mesh(MeshData data) {
        vertexCount = data.vertexCount();

        vboPos = upload(data.positions);
        vboSmooth = upload(data.smoothNormals);
        vboFlat = upload(data.flatNormals);
        vboUV = upload(data.uvs);

        vaoSmooth = buildVao(vboPos, vboSmooth, vboUV);
        vaoFlat = buildVao(vboPos, vboFlat, vboUV);
    }

    private static int upload(float[] data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        return vbo;
    }

    private static int buildVao(int pos, int normal, int uv) {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, pos);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, normal);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, uv);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        return vao;
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public int getTriangleCount() {
        return vertexCount / 3;
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoSmooth);
        glDeleteVertexArrays(vaoFlat);
        glDeleteBuffers(vboPos);
        glDeleteBuffers(vboSmooth);
        glDeleteBuffers(vboFlat);
        glDeleteBuffers(vboUV);
    }
}
