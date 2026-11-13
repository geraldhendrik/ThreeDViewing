# 3D Visual Realism Suite (Minggu 11 – Grafika Komputer)

Java 17+ · LWJGL 3.3.4 · JOML · OpenGL 3.3 core (GLSL 330) · Maven (NetBeans 21)

## Menjalankan di NetBeans 21
1. **File → Open Project…** → pilih folder `ThreeDViewing` (ikon Maven) → Open.
2. Pastikan JDK 17 atau 21 (Tools → Java Platforms). Pertama kali, NetBeans mengunduh LWJGL/JOML otomatis (butuh internet).
3. Klik kanan project → **Clean and Build**, lalu **Run Project (F6)**.
   Main class: `com.grafkom.viewing3d.Main`.
4. macOS: tambahkan VM option `-XstartOnFirstThread` (Project Properties → Run → VM Options).

## Kontrol
| Tombol | Fungsi | Indikator |
|---|---|---|
| B | Phong ↔ Blinn-Phong | 1 |
| F | Flat ↔ Smooth shading | 2 |
| Z / C | Z-Buffer / Back-Face Culling ON/OFF | 3 |
| T / X | Filter Nearest ↔ Bilinear+Mipmap / Tekstur ON/OFF | 4 |
| S | Shadow Mapping ON/OFF | 5 |
| N / M | Shadow bias dikecilkan / dibesarkan (lihat Shadow Acne & Peter Panning) | 5 |
| 1 / 2 / 3 | Sphere / Torus / Rumah 3D | – |
| L / R | Animasi cahaya / rotasi model | – |
| V | VSync ON/OFF (matikan untuk mengukur FPS) | laporan |
| Mouse drag, panah, scroll | Orbit & zoom kamera | – |

Status (FPS, mode shading, dll.) tampil di judul window.

## Struktur
- `Application` – window, input, render loop 2-pass (depth pass → lighting pass)
- `MeshData` / `MeshFactory` – geometri + normal per-face & vertex-normal averaging + UV
- `Mesh` – VAO Flat & VAO Smooth · `Shader` · `Texture2D` (STBImage) · `HSRManager` · `ShadowMapFBO` · `Camera`
- `resources/shaders` – `phong.vert/frag`, `depth.vert/frag` · `resources/textures` – `brick.png`, `checker.png`
