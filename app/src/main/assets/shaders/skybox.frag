#version 320 es
precision mediump float;

in vec3 v_texCoord;
out vec4 fragColor;

uniform float u_starDensity;
uniform float u_time;

float hash3D(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

void main() {
    vec3 dir = normalize(v_texCoord);
    vec3 color = vec3(0.001, 0.001, 0.015);

    vec3 gridPos = dir * 100.0;
    vec3 cell = floor(gridPos);

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 neighborCell = cell + vec3(float(x), float(y), float(z));
                float rand = hash3D(neighborCell);

                if (rand < u_starDensity * 0.01) {
                    vec3 starPos = neighborCell + vec3(
                        hash3D(neighborCell + 1.0),
                        hash3D(neighborCell + 2.0),
                        hash3D(neighborCell + 3.0)
                    );

                    float dist = length(gridPos - starPos);
                    float brightness = 1.0 / (1.0 + dist * dist * 500.0);

                    float colorRand = hash3D(neighborCell + 10.0);
                    vec3 starColor;
                    if (colorRand < 0.3) {
                        starColor = vec3(0.8, 0.85, 1.0);
                    } else if (colorRand < 0.6) {
                        starColor = vec3(1.0, 1.0, 0.9);
                    } else if (colorRand < 0.8) {
                        starColor = vec3(1.0, 0.9, 0.7);
                    } else {
                        starColor = vec3(1.0, 0.7, 0.5);
                    }

                    float twinkle = sin(u_time * (2.0 + rand * 3.0) + rand * 100.0) * 0.3 + 0.7;
                    color += starColor * brightness * twinkle * 3.0;
                }
            }
        }
    }

    float nebula = pow(max(1.0 - abs(dir.y), 0.0), 8.0) * 0.03;
    color += vec3(0.1, 0.05, 0.15) * nebula;

    fragColor = vec4(color, 1.0);
}
