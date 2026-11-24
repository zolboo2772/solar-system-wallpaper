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

void main() {
    vec3 baseColor = texture(u_diffuseMap, v_texCoord).rgb;

    // Moving solar convection granules
    vec2 uv1 = v_texCoord * 6.0 + vec2(u_time * 0.03, u_time * 0.02);
    vec2 uv2 = v_texCoord * 12.0 - vec2(u_time * 0.02, u_time * 0.03);
    float n1 = noise(uv1);
    float n2 = noise(uv2);
    float turbulence = (n1 * 0.6 + n2 * 0.4) * 0.25;

    vec3 hotColor = vec3(1.0, 0.96, 0.7);
    vec3 coolColor = vec3(1.0, 0.45, 0.1);
    vec3 sunColor = mix(coolColor, hotColor, clamp(baseColor.r + turbulence, 0.0, 1.0));

    // Corona rim glow
    vec3 viewDir = normalize(u_cameraPosition - v_worldPosition);
    float rim = 1.0 - max(dot(normalize(v_normal), viewDir), 0.0);
    rim = pow(rim, 2.5);
    vec3 coronaColor = vec3(1.0, 0.65, 0.2) * rim * 2.0;

    sunColor = sunColor * 1.8 + coronaColor;
    sunColor = sunColor / (sunColor + vec3(1.0));
    sunColor = pow(sunColor, vec3(1.0 / 2.2));

    fragColor = vec4(sunColor, 1.0);
}
