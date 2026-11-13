#version 330 core
// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Per-pixel lighting: Phong / Blinn-Phong + Texture + Shadow Mapping
// ==========================================================
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform float specularStrength;

uniform sampler2D textureSampler;   // unit 0
uniform sampler2D shadowMap;        // unit 2

uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform float shadowBiasScale;      // bias maksimum (slope-scaled)
uniform float shadowBiasMin;        // bias minimum

// Mengembalikan 1.0 (terkena cahaya) atau 0.2 (dalam bayangan), dengan PCF 3x3
float shadowFactor(vec4 fragPosLight, vec3 N, vec3 L) {
    vec3 p = fragPosLight.xyz / fragPosLight.w;   // perspective divide
    p = p * 0.5 + 0.5;                            // NDC [-1,1] -> [0,1]
    if (p.z > 1.0) return 1.0;                    // di luar far plane cahaya

    // bias dinamis berdasarkan kemiringan permukaan terhadap cahaya
    float bias = max(shadowBiasScale * (1.0 - dot(N, L)), shadowBiasMin);

    float inShadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float dShadow = texture(shadowMap, p.xy + vec2(x, y) * texel).r;
            inShadow += (p.z - bias > dShadow) ? 1.0 : 0.0;   // d_current > d_shadow + bias
        }
    }
    inShadow /= 9.0;
    return mix(1.0, 0.2, inShadow);
}

void main() {
    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
    }
    // spec harus 0 bila permukaan membelakangi cahaya
    if (diff <= 0.0) spec = 0.0;
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow (ambient tidak ikut digelapkan)
    float shadow = useShadow ? shadowFactor(FragPosLightSpace, norm, lightDir) : 1.0;

    // Combine Texture & Lighting
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadow * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
