#version 320 es
precision highp float;

in vec3 v_worldPosition;
in vec3 v_localPosition;
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

vec3 safeNorm(vec3 v) {
    float d = dot(v, v);
    return d > 1e-8 ? v * inversesqrt(d) : vec3(0.0, 1.0, 0.0);
}

vec3 toLinear(vec3 c) {
    return pow(max(c, vec3(1e-5)), vec3(2.2));
}

float hash31(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise3D(vec3 p) {
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

float fbmMicro(vec3 p) {
    float v = 0.0;
    v += 0.65 * noise3D(p);
    v += 0.35 * noise3D(p * 2.13);
    return v;
}

float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = max(roughness, 0.05) * max(roughness, 0.05);
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    return a2 / max(PI * denom * denom, 1e-5);
}

float geometrySchlickGGX(float NdotV, float roughness) {
    float r = max(roughness, 0.05) + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / max(NdotV * (1.0 - k) + k, 1e-5);
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    return geometrySchlickGGX(NdotV, roughness) * geometrySchlickGGX(NdotL, roughness);
}

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    float factor = clamp(1.0 - cosTheta, 0.0, 1.0);
    float f2 = factor * factor;
    return F0 + (1.0 - F0) * (f2 * f2 * factor);
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
    vec3 V = safeNorm(v_viewDir);
    vec3 L = safeNorm(-u_lightDirection);
    vec3 baseNormal = safeNorm(v_normal);
    vec3 sphereDir = safeNorm(v_localPosition);

    vec3 N;
    if (u_hasNormalMap > 0.5) {
        vec3 normalSample = texture(u_normalMap, v_texCoord).rgb * 2.0 - 1.0;
        mat3 tbn = mat3(safeNorm(v_tangent), safeNorm(v_bitangent), baseNormal);
        N = safeNorm(tbn * normalSample);
    } else {
        N = baseNormal;
    }

    vec4 diffuseSample = texture(u_diffuseMap, v_texCoord);
    vec3 albedo = toLinear(diffuseSample.rgb);

    // Seamless 3D spherical micro-detail rendered at native screen resolution (zero pixelation on close zoom)
    float micro1 = fbmMicro(sphereDir * 36.0);
    float micro2 = noise3D(sphereDir * 95.0);
    float fineRelief = (micro1 * 0.65 + micro2 * 0.35) - 0.5;
    float isOcean = (u_hasNightMap > 0.5 && diffuseSample.a < 0.5) ? 1.0 : 0.0;

    albedo = clamp(albedo * (1.0 + fineRelief * (0.28 - isOcean * 0.24)), 0.0, 1.0);

    // Perturb dry land/rock normal slightly using high-frequency GPU noise
    if (isOcean < 0.5) {
        vec3 eps = vec3(micro1 - 0.5, micro2 - 0.5, 0.0) * 0.12;
        N = safeNorm(N + safeNorm(v_tangent) * eps.x + safeNorm(v_bitangent) * eps.y);
    }

    float effectiveRoughness = mix(u_roughness, 0.15, isOcean);

    float cloudAlpha = 0.0;
    float cloudShadow = 1.0;
    if (u_hasCloudMap > 0.5) {
        vec2 cloudUV = vec2(v_texCoord.x + u_cloudOffset, v_texCoord.y);
        float texCloud = texture(u_cloudMap, cloudUV).r;
        float fineCloud = fbmMicro(sphereDir * 24.0 + vec3(u_cloudOffset * 6.0, 0.0, 0.0));
        cloudAlpha = clamp(texCloud * (0.7 + 0.6 * fineCloud), 0.0, 1.0);

        // Soft cloud shadow projected slightly opposite light direction
        float shadowSample = texture(u_cloudMap, cloudUV - vec2(0.004, 0.002)).r;
        cloudShadow = 1.0 - shadowSample * 0.42;

        albedo *= mix(1.0, cloudShadow, 1.0 - cloudAlpha * 0.6);
        albedo = mix(albedo, vec3(0.96, 0.97, 0.99), cloudAlpha * 0.85);
        effectiveRoughness = mix(effectiveRoughness, 0.90, cloudAlpha * 0.85);
    }

    vec3 H = safeNorm(V + L);
    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);

    vec3 F0 = mix(vec3(0.04), albedo, u_metallic);
    float D = distributionGGX(N, H, effectiveRoughness);
    float G = geometrySmith(N, V, L, effectiveRoughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator = D * G * F;
    float denominator = max(4.0 * NdotV * NdotL, 0.001);
    vec3 specular = numerator / denominator;

    vec3 kD = (vec3(1.0) - F) * (1.0 - u_metallic);
    // Subtle dark-side cosmic ambient + soft terminator wrap
    vec3 ambient = max(u_ambientStrength, 0.03) * albedo;
    float softWrap = smoothstep(-0.18, 0.25, dot(baseNormal, L)) * 0.06;
    vec3 color = ambient + albedo * softWrap + (kD * albedo / PI + specular) * u_lightColor * 2.25 * NdotL;

    if (u_hasNightMap > 0.5) {
        float nightFactor = smoothstep(0.02, 0.28, -dot(baseNormal, L));
        vec3 rawNight = texture(u_nightMap, v_texCoord).rgb;
        vec3 nightLights = toLinear(rawNight) * 3.8;
        color += nightLights * nightFactor * (1.0 - cloudAlpha * 0.72);
    }

    if (u_atmosphereIntensity > 0.0) {
        float rim = clamp(1.0 - max(dot(baseNormal, V), 0.0), 0.0, 1.0);
        float fresnelRim = rim * rim * rim;
        float outerHaze = rim * rim * 0.25;
        float sunFacing = smoothstep(-0.30, 0.65, dot(baseNormal, L));
        vec3 atmoLinear = toLinear(u_atmosphereColor);
        vec3 atmosphere = atmoLinear * (fresnelRim + outerHaze) * (0.14 + 0.86 * sunFacing) * u_atmosphereIntensity * 1.45;
        color += atmosphere;
    }

    color = acesFilm(color);
    color = pow(max(color, vec3(1e-5)), vec3(1.0 / 2.2));

    fragColor = vec4(color, 1.0);
}
