#version 320 es
precision mediump float;

in vec3 v_worldPosition;
in vec3 v_normal;
in vec2 v_texCoord;
in vec3 v_viewDir;
in mat3 v_tbn;

uniform sampler2D u_diffuseMap;
uniform sampler2D u_normalMap;
uniform sampler2D u_nightMap;
uniform sampler2D u_cloudMap;

uniform vec3 u_lightDirection;
uniform vec3 u_lightColor;
uniform vec3 u_atmosphereColor;
uniform float u_atmosphereIntensity;
uniform float u_hasNormalMap;
uniform float u_hasNightMap;
uniform float u_hasCloudMap;
uniform float u_cloudOffset;
uniform float u_roughness;
uniform float u_metallic;
uniform float u_ambientStrength;

out vec4 fragColor;

const float PI = 3.14159265359;

float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    return a2 / (PI * denom * denom);
}

float geometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    return geometrySchlickGGX(NdotV, roughness) * geometrySchlickGGX(NdotL, roughness);
}

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

void main() {
    vec3 V = normalize(v_viewDir);
    vec3 L = normalize(-u_lightDirection);

    vec3 N;
    if (u_hasNormalMap > 0.5) {
        vec3 normalSample = texture(u_normalMap, v_texCoord).rgb * 2.0 - 1.0;
        N = normalize(v_tbn * normalSample);
    } else {
        N = normalize(v_normal);
    }

    vec3 albedo = texture(u_diffuseMap, v_texCoord).rgb;

    if (u_hasCloudMap > 0.5) {
        vec2 cloudUV = vec2(v_texCoord.x + u_cloudOffset, v_texCoord.y);
        float cloudAlpha = texture(u_cloudMap, cloudUV).r;
        albedo = mix(albedo, vec3(1.0), cloudAlpha * 0.7);
    }

    vec3 H = normalize(V + L);
    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);

    vec3 F0 = mix(vec3(0.04), albedo, u_metallic);
    float D = distributionGGX(N, H, u_roughness);
    float G = geometrySmith(N, V, L, u_roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator = D * G * F;
    float denominator = 4.0 * NdotV * NdotL + 0.0001;
    vec3 specular = numerator / denominator;

    vec3 kD = (vec3(1.0) - F) * (1.0 - u_metallic);
    vec3 ambient = u_ambientStrength * albedo;
    vec3 color = ambient + (kD * albedo / PI + specular) * u_lightColor * NdotL;

    if (u_hasNightMap > 0.5) {
        float nightFactor = smoothstep(0.0, 0.2, -dot(normalize(v_normal), L));
        vec3 nightLights = texture(u_nightMap, v_texCoord).rgb * 2.0;
        color += nightLights * nightFactor;
    }

    if (u_atmosphereIntensity > 0.0) {
        float rim = 1.0 - max(dot(N, V), 0.0);
        rim = pow(rim, 3.0);
        float scattering = max(dot(N, L), 0.0) * 0.6 + 0.4;
        vec3 atmosphere = u_atmosphereColor * rim * scattering * u_atmosphereIntensity;
        color += atmosphere;
    }

    color = color / (color + vec3(1.0));
    color = pow(color, vec3(1.0 / 2.2));

    fragColor = vec4(color, 1.0);
}
