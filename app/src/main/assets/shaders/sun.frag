#version 320 es
precision highp float;

in vec2 v_texCoord;
in vec3 v_normal;
in vec3 v_worldPosition;

uniform sampler2D u_diffuseMap;
uniform float u_time;
uniform vec3 u_cameraPosition;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(0.3183099, 0.3678794) + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * (p.x + p.y));
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
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
    vec3 baseColor = pow(max(texture(u_diffuseMap, v_texCoord).rgb, vec3(1e-5)), vec3(2.2));

    vec2 uv1 = v_texCoord * 10.0 + vec2(u_time * 0.025, u_time * 0.015);
    vec2 uv2 = v_texCoord * 22.0 - vec2(u_time * 0.018, u_time * 0.024);
    float n1 = noise(uv1);
    float n2 = noise(uv2);
    float turbulence = (n1 * 0.6 + n2 * 0.4) * 0.35;

    vec3 hotColor = vec3(1.0, 0.94, 0.55);
    vec3 coolColor = vec3(0.95, 0.24, 0.03);
    vec3 sunColor = mix(coolColor, hotColor, clamp(baseColor.r * 0.85 + turbulence, 0.0, 1.0));

    vec3 viewDir = normalize(u_cameraPosition - v_worldPosition);
    float NdotV = max(dot(normalize(v_normal), viewDir), 0.0);
    float rim = clamp(1.0 - NdotV, 0.0, 1.0);
    float corona = rim * rim * rim;
    vec3 coronaColor = vec3(1.0, 0.52, 0.12) * corona * 2.6;

    // Realistic solar limb darkening
    float limb = mix(0.45, 1.0, pow(max(NdotV, 1e-4), 0.45));
    sunColor = sunColor * 2.2 * limb + coronaColor;
    sunColor = acesFilm(sunColor);
    sunColor = pow(max(sunColor, vec3(1e-5)), vec3(1.0 / 2.2));

    fragColor = vec4(sunColor, 1.0);
}
