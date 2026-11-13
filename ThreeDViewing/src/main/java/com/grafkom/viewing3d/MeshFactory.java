package com.grafkom.viewing3d;

/**
 * Pabrik model 3D prosedural (Sphere, Torus, Rumah 3D, dan bidang lantai).
 * Semua model punya koordinat vertex, vektor normal, dan koordinat tekstur UV.
 * Resolusi sphere/torus sengaja dibuat rendah agar perbedaan Flat vs Smooth terlihat jelas.
 */
public final class MeshFactory {

    private MeshFactory() {}

    // ------------------------------------------------------------------ Sphere
    public static MeshData sphere(float r, int stacks, int slices) {
        MeshData.Builder b = new MeshData.Builder();
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                float[] A = spherePoint(r, i, j, stacks, slices);
                float[] B = spherePoint(r, i, j + 1, stacks, slices);
                float[] C = spherePoint(r, i + 1, j, stacks, slices);
                float[] D = spherePoint(r, i + 1, j + 1, stacks, slices);

                float u0 = (float) j / slices, u1 = (float) (j + 1) / slices;
                float v0 = (float) i / stacks, v1 = (float) (i + 1) / stacks;

                b.triangle(A, C, B, new float[]{u0, v0}, new float[]{u0, v1}, new float[]{u1, v0});
                b.triangle(B, C, D, new float[]{u1, v0}, new float[]{u0, v1}, new float[]{u1, v1});
            }
        }
        return b.build();
    }

    private static float[] spherePoint(float r, int i, int j, int stacks, int slices) {
        double lat = -Math.PI / 2 + Math.PI * i / stacks;
        double lon = 2 * Math.PI * j / slices;
        return new float[]{
            (float) (r * Math.cos(lat) * Math.cos(lon)),
            (float) (r * Math.sin(lat)),
            (float) (r * Math.cos(lat) * Math.sin(lon))};
    }

    // ------------------------------------------------------------------ Torus
    public static MeshData torus(float R, float r, int rings, int sides) {
        MeshData.Builder b = new MeshData.Builder();
        float uRep = 4f, vRep = 2f; // pengulangan tekstur agar tidak melar
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                float[] A = torusPoint(R, r, i, j, rings, sides);
                float[] B = torusPoint(R, r, i + 1, j, rings, sides);
                float[] C = torusPoint(R, r, i, j + 1, rings, sides);
                float[] D = torusPoint(R, r, i + 1, j + 1, rings, sides);

                float u0 = uRep * i / rings, u1 = uRep * (i + 1) / rings;
                float v0 = vRep * j / sides, v1 = vRep * (j + 1) / sides;

                b.triangle(A, C, B, new float[]{u0, v0}, new float[]{u0, v1}, new float[]{u1, v0});
                b.triangle(B, C, D, new float[]{u1, v0}, new float[]{u0, v1}, new float[]{u1, v1});
            }
        }
        return b.build();
    }

    private static float[] torusPoint(float R, float r, int i, int j, int rings, int sides) {
        double u = 2 * Math.PI * i / rings;
        double v = 2 * Math.PI * j / sides;
        return new float[]{
            (float) ((R + r * Math.cos(v)) * Math.cos(u)),
            (float) (r * Math.sin(v)),
            (float) ((R + r * Math.cos(v)) * Math.sin(u))};
    }

    // ------------------------------------------------------------------ Rumah 3D
    public static MeshData house() {
        MeshData.Builder b = new MeshData.Builder();
        final float x = 1.2f, z = 1.0f, yb = -1.3f, ye = 0.3f; // badan rumah
        final float rx = 1.4f, rz = 1.2f, yr = 1.3f;           // atap
        float[] inBody = {0, (yb + ye) / 2, 0};
        float[] inRoof = {0, (ye + yr) / 2, 0};

        float[][] uvQuad = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        float[][] uvRoof = {{0, 0}, {2, 0}, {2, 1}, {0, 1}};

        // dinding depan (+z), belakang (-z), kanan (+x), kiri (-x), alas (-y)
        b.quadOutward(new float[][]{{-x, yb, z}, {x, yb, z}, {x, ye, z}, {-x, ye, z}}, uvQuad, inBody);
        b.quadOutward(new float[][]{{x, yb, -z}, {-x, yb, -z}, {-x, ye, -z}, {x, ye, -z}}, uvQuad, inBody);
        b.quadOutward(new float[][]{{x, yb, z}, {x, yb, -z}, {x, ye, -z}, {x, ye, z}}, uvQuad, inBody);
        b.quadOutward(new float[][]{{-x, yb, -z}, {-x, yb, z}, {-x, ye, z}, {-x, ye, -z}}, uvQuad, inBody);
        b.quadOutward(new float[][]{{-x, yb, -z}, {x, yb, -z}, {x, yb, z}, {-x, yb, z}}, uvQuad, inBody);

        // atap: dua sisi miring + dua segitiga gable + alas atap
        b.quadOutward(new float[][]{{-rx, ye, rz}, {rx, ye, rz}, {rx, yr, 0}, {-rx, yr, 0}}, uvRoof, inRoof);
        b.quadOutward(new float[][]{{rx, ye, -rz}, {-rx, ye, -rz}, {-rx, yr, 0}, {rx, yr, 0}}, uvRoof, inRoof);
        b.triangleOutward(new float[]{rx, ye, rz}, new float[]{rx, ye, -rz}, new float[]{rx, yr, 0},
                new float[]{0, 0}, new float[]{1, 0}, new float[]{0.5f, 1}, inRoof);
        b.triangleOutward(new float[]{-rx, ye, -rz}, new float[]{-rx, ye, rz}, new float[]{-rx, yr, 0},
                new float[]{0, 0}, new float[]{1, 0}, new float[]{0.5f, 1}, inRoof);
        b.quadOutward(new float[][]{{-rx, ye, -rz}, {rx, ye, -rz}, {rx, ye, rz}, {-rx, ye, rz}}, uvQuad, inRoof);
        return b.build();
    }

    // ------------------------------------------------------------------ Lantai
    /** Bidang datar di y = 0 dengan sisi 2*half, tekstur diulang uvRepeat kali. */
    public static MeshData plane(float half, float uvRepeat) {
        MeshData.Builder b = new MeshData.Builder();
        float[][] p = {{-half, 0, -half}, {half, 0, -half}, {half, 0, half}, {-half, 0, half}};
        float[][] t = {{0, 0}, {uvRepeat, 0}, {uvRepeat, uvRepeat}, {0, uvRepeat}};
        b.quadOutward(p, t, new float[]{0, -1, 0}); // normal menghadap ke atas
        return b.build();
    }
}
