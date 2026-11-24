#version 320 es
precision mediump float;

in vec2 v_texCoord;
in vec3 v_normal;
in vec3 v_worldPosition;

uniform sampler2D u_diffuseMap;
uniform float u_time;
uniform vec3 u_cameraPosition;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
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

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec3 baseColor = texture(u_diffuseMap, v_texCoord).rgb;

    vec2 animUV = v_texCoord * 4.0 + vec2(u_time * 0.02, u_time * 0.01);
    float turbulence = fbm(animUV) * 0.3;

    vec3 hotColor = vec3(1.0, 0.95, 0.6);
    vec3 coolColor = vec3(1.0, 0.4, 0.1);
    vec3 sunColor = mix(coolColor, hotColor, baseColor.r + turbulence);

    sunColor *= 2.5;

    vec3 viewDir = normalize(u_cameraPosition - v_worldPosition);
    float rim = 1.0 - max(dot(normalize(v_normal), viewDir), 0.0);
    rim = pow(rim, 2.0);
    vec3 coronaColor = vec3(1.0, 0.6, 0.2) * rim * 3.0;
    sunColor += coronaColor;

    sunColor = sunColor / (sunColor + vec3(1.0));
    sunColor = pow(sunColor, vec3(1.0 / 2.2));

    fragColor = vec4(sunColor, 1.0);
}
