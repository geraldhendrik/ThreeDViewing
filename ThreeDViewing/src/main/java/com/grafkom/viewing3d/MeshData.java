package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data mesh murni (tanpa OpenGL): posisi, normal, dan UV dalam bentuk "triangle soup"
 * (tiap segitiga punya 3 vertex sendiri).
 *
 * Dua set normal dihitung sekaligus:
 *  - flatNormals   : satu normal per face (Flat Shading)
 *  - smoothNormals : rata-rata normal semua face yang berbagi posisi vertex yang sama
 *                    (Smooth Shading), N_vertex = sum(N_face) / ||sum(N_face)||
 */
public final class MeshData {

    public final float[] positions;     // 3 float per vertex
    public final float[] flatNormals;   // 3 float per vertex
    public final float[] smoothNormals; // 3 float per vertex
    public final float[] uvs;           // 2 float per vertex

    private MeshData(float[] p, float[] fn, float[] sn, float[] uv) {
        this.positions = p;
        this.flatNormals = fn;
        this.smoothNormals = sn;
        this.uvs = uv;
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    // ------------------------------------------------------------------ Builder
    public static final class Builder {
        private final List<float[]> pos = new ArrayList<>(); // tiap elemen: 3 titik (9 float)
        private final List<float[]> uv = new ArrayList<>();  // tiap elemen: 3 uv (6 float)

        /** Tambah segitiga dengan urutan CCW (tampak depan). Segitiga degenerate diabaikan. */
        public Builder triangle(float[] a, float[] b, float[] c, float[] ua, float[] ub, float[] uc) {
            float[] n = cross(sub(b, a), sub(c, a));
            if (length(n) < 1e-9f) return this; // degenerate (mis. di kutub bola)
            pos.add(new float[]{a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]});
            uv.add(new float[]{ua[0], ua[1], ub[0], ub[1], uc[0], uc[1]});
            return this;
        }

        /**
         * Segitiga yang otomatis dibalik jika normalnya menghadap ke titik 'inside'
         * sehingga selalu CCW dilihat dari luar objek.
         */
        public Builder triangleOutward(float[] a, float[] b, float[] c,
                                       float[] ua, float[] ub, float[] uc, float[] inside) {
            float[] n = cross(sub(b, a), sub(c, a));
            float[] ctr = {(a[0] + b[0] + c[0]) / 3f, (a[1] + b[1] + c[1]) / 3f, (a[2] + b[2] + c[2]) / 3f};
            if (dot(n, sub(ctr, inside)) < 0) return triangle(a, c, b, ua, uc, ub);
            return triangle(a, b, c, ua, ub, uc);
        }

        /** Quad (p0..p3 berurutan mengelilingi bidang), UV tiap sudut diberikan. */
        public Builder quadOutward(float[][] p, float[][] t, float[] inside) {
            triangleOutward(p[0], p[1], p[2], t[0], t[1], t[2], inside);
            triangleOutward(p[0], p[2], p[3], t[0], t[2], t[3], inside);
            return this;
        }

        public MeshData build() {
            int tris = pos.size();
            int verts = tris * 3;
            float[] p = new float[verts * 3];
            float[] t = new float[verts * 2];
            float[] fn = new float[verts * 3];
            float[] sn = new float[verts * 3];

            Map<String, float[]> accum = new HashMap<>();

            for (int i = 0; i < tris; i++) {
                float[] tp = pos.get(i);
                float[] tt = uv.get(i);
                float[] a = {tp[0], tp[1], tp[2]};
                float[] b = {tp[3], tp[4], tp[5]};
                float[] c = {tp[6], tp[7], tp[8]};
                float[] n = normalize(cross(sub(b, a), sub(c, a)));

                for (int k = 0; k < 3; k++) {
                    int v = i * 3 + k;
                    System.arraycopy(tp, k * 3, p, v * 3, 3);
                    System.arraycopy(tt, k * 2, t, v * 2, 2);
                    fn[v * 3] = n[0];
                    fn[v * 3 + 1] = n[1];
                    fn[v * 3 + 2] = n[2];

                    float[] s = accum.computeIfAbsent(key(tp[k * 3], tp[k * 3 + 1], tp[k * 3 + 2]),
                            x -> new float[3]);
                    s[0] += n[0];
                    s[1] += n[1];
                    s[2] += n[2];
                }
            }

            for (int v = 0; v < verts; v++) {
                float[] s = accum.get(key(p[v * 3], p[v * 3 + 1], p[v * 3 + 2]));
                float[] n = normalize(s);
                sn[v * 3] = n[0];
                sn[v * 3 + 1] = n[1];
                sn[v * 3 + 2] = n[2];
            }
            return new MeshData(p, fn, sn, t);
        }

        // vertex dengan posisi sama (toleransi 1e-3) dianggap vertex yang sama
        private static String key(float x, float y, float z) {
            return Math.round(x * 1000f) + "," + Math.round(y * 1000f) + "," + Math.round(z * 1000f);
        }
    }

    // ------------------------------------------------------------------ vektor kecil
    static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static float[] cross(float[] a, float[] b) {
        return new float[]{
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]};
    }

    static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static float length(float[] a) {
        return (float) Math.sqrt(dot(a, a));
    }

    static float[] normalize(float[] a) {
        float l = length(a);
        if (l < 1e-9f) return new float[]{0, 1, 0};
        return new float[]{a[0] / l, a[1] / l, a[2] / l};
    }
}
