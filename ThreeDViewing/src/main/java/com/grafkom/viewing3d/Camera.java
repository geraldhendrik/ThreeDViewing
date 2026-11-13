package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit: berputar mengelilingi titik target (mouse drag / tombol panah, scroll = zoom). */
public class Camera {

    private float yaw = 40f;     // derajat, putaran horizontal
    private float pitch = 28f;   // derajat, putaran vertikal
    private float distance = 9f;
    private final Vector3f target = new Vector3f(0f, -0.4f, 0f);

    public void rotate(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(-10f, Math.min(89f, pitch + dPitch));
    }

    public void zoom(float amount) {
        distance = Math.max(3f, Math.min(30f, distance - amount));
    }

    public Vector3f getPosition() {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f(
                target.x + distance * (float) (Math.cos(p) * Math.sin(y)),
                target.y + distance * (float) Math.sin(p),
                target.z + distance * (float) (Math.cos(p) * Math.cos(y)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0, 1, 0));
    }
}
