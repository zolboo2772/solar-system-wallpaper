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

void main() {
    vec3 dir = normalize(v_texCoord);

    // Subtle galactic deep space ambient
    float galacticBand = pow(max(1.0 - abs(dir.y), 0.0), 4.0);
    vec3 spaceColor = vec3(0.003, 0.004, 0.015) + vec3(0.025, 0.012, 0.045) * galacticBand;

    // Fast spherical grid starfield
    vec3 grid = dir * 180.0;
    vec3 cell = floor(grid);
    vec3 f = fract(grid) - 0.5;

    float rnd = hash(cell);
    float star = 0.0;
    vec3 starColor = vec3(1.0);

    float threshold = 1.0 - (u_starDensity * 0.0004 + 0.008);
    if (rnd > threshold) {
        vec2 offset = vec2(hash(cell + 1.0), hash(cell + 2.0)) - 0.5;
        float dist = length(f.xy - offset * 0.6);
        float brightness = smoothstep(0.12, 0.0, dist);

        float twinkle = 0.65 + 0.35 * sin(u_time * (2.0 + rnd * 4.0) + rnd * 62.0);

        if (rnd > 0.985) {
            starColor = vec3(0.75, 0.88, 1.0); // Blue-white
        } else if (rnd > 0.95) {
            starColor = vec3(1.0, 0.96, 0.85); // Warm white
        } else {
            starColor = vec3(1.0, 0.78, 0.55); // Orange/Red dwarf
        }

        star = brightness * twinkle * 2.5;
    }

    vec3 finalColor = spaceColor + starColor * star;
    fragColor = vec4(finalColor, 1.0);
}
