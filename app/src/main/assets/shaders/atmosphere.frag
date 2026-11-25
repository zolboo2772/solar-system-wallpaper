#version 320 es
precision highp float;

in vec3 v_worldNormal;
in vec3 v_viewDir;

uniform vec3 u_color;
uniform vec3 u_lightDirection;
uniform float u_intensity;
uniform float u_power;
uniform float u_isSun;

out vec4 fragColor;

vec3 safeNorm(vec3 v) {
    float d = dot(v, v);
    return d > 1e-8 ? v * inversesqrt(d) : vec3(0.0, 1.0, 0.0);
}

void main() {
    vec3 N = safeNorm(v_worldNormal);
    vec3 V = safeNorm(v_viewDir);
    // Back-face sphere shell: dot(-N, V) approaches 0.0 cleanly at the outer limb edge
    float rimGlow = clamp(dot(-N, V), 0.0, 1.0);
    float falloff = pow(max(rimGlow, 1e-5), u_power);

    float lightMod = 1.0;
    if (u_isSun < 0.5) {
        vec3 L = safeNorm(-u_lightDirection);
        lightMod = smoothstep(-0.35, 0.55, dot(N, L)) * 0.88 + 0.12;
    }

    float alpha = clamp(falloff * u_intensity * lightMod, 0.0, 1.0);
    fragColor = vec4(u_color * alpha, alpha);
}
