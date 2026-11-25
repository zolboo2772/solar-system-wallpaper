#version 320 es
precision highp float;

in vec3 v_worldPosition;
in vec3 v_normal;
in vec2 v_texCoord;
in vec3 v_viewDir;
in vec3 v_tangent;
in vec3 v_bitangent;

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

float hash31(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float microNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

vec3 acesFilm(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 V = normalize(v_viewDir);
    vec3 L = normalize(-u_lightDirection);
    vec3 baseNormal = normalize(v_normal);

    vec3 N;
    if (u_hasNormalMap > 0.5) {
        vec3 normalSample = texture(u_normalMap, v_texCoord).rgb * 2.0 - 1.0;
        mat3 tbn = mat3(normalize(v_tangent), normalize(v_bitangent), baseNormal);
        N = normalize(tbn * normalSample);
    } else {
        N = baseNormal;
    }

    vec4 diffuseSample = texture(u_diffuseMap, v_texCoord);
    vec3 albedo = pow(diffuseSample.rgb, vec3(2.2));

    // GPU micro-detail for crispness at close-up Xiaomi zoom
    float detail = microNoise(vec3(v_texCoord * vec2(120.0, 60.0), 1.0)) * 0.14 - 0.07;
    float isOcean = (u_hasNightMap > 0.5 && diffuseSample.a < 0.5) ? 1.0 : 0.0;
    albedo = clamp(albedo * (1.0 + detail * (1.0 - isOcean * 0.75)), 0.0, 1.0);

    float effectiveRoughness = mix(u_roughness, 0.16, isOcean);

    float cloudAlpha = 0.0;
    if (u_hasCloudMap > 0.5) {
        vec2 cloudUV = vec2(v_texCoord.x + u_cloudOffset, v_texCoord.y);
        cloudAlpha = texture(u_cloudMap, cloudUV).r;
        albedo = mix(albedo, vec3(0.95), cloudAlpha * 0.78);
        effectiveRoughness = mix(effectiveRoughness, 0.92, cloudAlpha * 0.8);
    }

    vec3 H = normalize(V + L);
    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);

    vec3 F0 = mix(vec3(0.04), albedo, u_metallic);
    float D = distributionGGX(N, H, effectiveRoughness);
    float G = geometrySmith(N, V, L, effectiveRoughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator = D * G * F;
    float denominator = 4.0 * NdotV * NdotL + 0.0001;
    vec3 specular = numerator / denominator;

    vec3 kD = (vec3(1.0) - F) * (1.0 - u_metallic);
    vec3 ambient = u_ambientStrength * 0.65 * albedo;
    vec3 color = ambient + (kD * albedo / PI + specular) * u_lightColor * 2.1 * NdotL;

    if (u_hasNightMap > 0.5) {
        float nightFactor = smoothstep(0.02, 0.25, -dot(baseNormal, L));
        vec3 nightLights = pow(texture(u_nightMap, v_texCoord).rgb, vec3(2.2)) * 3.2;
        color += nightLights * nightFactor * (1.0 - cloudAlpha * 0.7);
    }

    if (u_atmosphereIntensity > 0.0) {
        float rim = 1.0 - max(dot(baseNormal, V), 0.0);
        float fresnelRim = pow(rim, 3.2);
        float sunFacing = smoothstep(-0.25, 0.75, dot(baseNormal, L));
        vec3 atmoLinear = pow(u_atmosphereColor, vec3(2.2));
        vec3 atmosphere = atmoLinear * fresnelRim * (0.12 + 0.88 * sunFacing) * u_atmosphereIntensity * 1.4;
        color += atmosphere;
    }

    color = acesFilm(color);
    color = pow(color, vec3(1.0 / 2.2));

    fragColor = vec4(color, 1.0);
}
