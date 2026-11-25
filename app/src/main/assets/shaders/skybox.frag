#version 320 es
precision highp float;

in vec3 v_texCoord;
out vec4 fragColor;

uniform float u_starDensity;
uniform float u_time;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash(i);
    float n100 = hash(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

float starLayer(vec3 dir, float scale, float densityBoost, float sizeScale, float timePhase) {
    vec3 grid = dir * scale;
    vec3 cell = floor(grid);
    vec3 f = fract(grid) - 0.5;
    float rnd = hash(cell);
    float threshold = 1.0 - (u_starDensity * 0.00035 + densityBoost);
    if (rnd <= threshold) return 0.0;

    vec2 offset = vec2(hash(cell + 1.1), hash(cell + 2.3)) - 0.5;
    float dist = length(f.xy - offset * 0.55);
    float brightness = smoothstep(sizeScale, 0.0, dist);
    float twinkle = 0.72 + 0.28 * sin(u_time * (1.8 + rnd * 3.5) + rnd * 50.0 + timePhase);
    return brightness * brightness * twinkle;
}

void main() {
    vec3 dir = normalize(v_texCoord);

    // Tilted Milky Way galactic plane + cosmic dust noise
    vec3 mwDir = normalize(vec3(dir.x, dir.y * 0.82 - dir.z * 0.57, dir.y * 0.57 + dir.z * 0.82));
    float band = clamp(1.0 - abs(mwDir.y), 0.0, 1.0);
    float bandPow = band * band * band * band;
    float dust = noise3(mwDir * 3.5) * 0.65 + noise3(mwDir * 7.5) * 0.35;
    float rift = smoothstep(0.32, 0.68, dust);

    vec3 deepSpace = vec3(0.004, 0.006, 0.016);
    vec3 nebula = mix(vec3(0.014, 0.022, 0.055), vec3(0.038, 0.026, 0.072), dust) * bandPow * rift;
    vec3 spaceColor = deepSpace + nebula;

    // Layer 1: faint dense field stars
    float s1 = starLayer(dir, 220.0, 0.012, 0.14, 0.0) * 1.1;
    // Layer 2: brighter colored stars
    vec3 grid2 = dir * 115.0;
    vec3 cell2 = floor(grid2);
    float rnd2 = hash(cell2 + 7.7);
    float s2 = starLayer(dir + 0.03, 115.0, 0.005, 0.11, 3.1) * 2.4;

    vec3 starTint = vec3(0.95, 0.97, 1.0);
    if (rnd2 > 0.988) {
        starTint = vec3(0.70, 0.86, 1.0);
    } else if (rnd2 > 0.96) {
        starTint = vec3(1.0, 0.92, 0.78);
    } else if (rnd2 > 0.93) {
        starTint = vec3(1.0, 0.75, 0.52);
    }

    vec3 finalColor = spaceColor + vec3(0.85, 0.90, 1.0) * s1 + starTint * s2;
    fragColor = vec4(finalColor, 1.0);
}
