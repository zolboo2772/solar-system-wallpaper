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

    vec3 linearRing = pow(ringColor.rgb, vec3(2.2));
    vec3 L = normalize(-u_lightDirection);
    float light = 0.85 + 0.25 * abs(L.y);

    // Accurate cylindrical shadow cast by Saturn onto its rings
    vec3 toFrag = v_worldPosition - u_planetCenter;
    float alongLight = dot(toFrag, L);
    if (alongLight < 0.0) {
        float perpDist = length(toFrag - L * alongLight);
        float shadow = smoothstep(u_planetRadius * 0.96, u_planetRadius * 1.04, perpDist);
        light *= mix(0.08, 1.0, shadow);
    }

    vec3 lit = linearRing * light;
    lit = pow(lit, vec3(1.0 / 2.2));
    fragColor = vec4(lit, ringColor.a);
}
