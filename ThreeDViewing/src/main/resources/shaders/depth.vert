#version 330 core
// Render Pass 1 (Depth Pass): scene digambar dari sudut pandang cahaya
layout (location = 0) in vec3 aPos;

uniform mat4 model;
uniform mat4 lightSpaceMatrix;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}
