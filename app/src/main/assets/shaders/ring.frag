#version 320 es
precision mediump float;

in vec2 v_texCoord;
in vec3 v_worldPosition;

uniform sampler2D u_ringTexture;
uniform vec3 u_planetCenter;
uniform float u_planetRadius;
uniform vec3 u_lightDirection;

out vec4 fragColor;

void main() {
    vec4 ringColor = texture(u_ringTexture, v_texCoord);
    if (ringColor.a < 0.01) discard;

    vec3 L = normalize(-u_lightDirection);
    float light = max(dot(vec3(0.0, 1.0, 0.0), L), 0.0) * 0.5 + 0.5;

    vec3 toFrag = v_worldPosition - u_planetCenter;
    float distFromAxis = length(toFrag.xz);
    float projOnLight = dot(normalize(toFrag), L);
    if (distFromAxis < u_planetRadius && projOnLight < 0.0) {
        light *= 0.15;
    }

    ringColor.rgb *= light;
    fragColor = ringColor;
}
