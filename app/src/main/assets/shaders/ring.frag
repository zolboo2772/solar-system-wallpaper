#version 320 es
precision highp float;

in vec2 v_texCoord;
in vec3 v_worldPosition;

uniform sampler2D u_ringTexture;
uniform vec3 u_planetCenter;
uniform float u_planetRadius;
uniform vec3 u_lightDirection;

out vec4 fragColor;

void main() {
    vec4 ringColor = texture(u_ringTexture, v_texCoord);
    if (ringColor.a < 0.02) discard;

    vec3 linearRing = pow(max(ringColor.rgb, vec3(1e-5)), vec3(2.2));
    vec3 L = normalize(-u_lightDirection);
    float light = 0.95 + 0.20 * abs(L.y);

    // Cylindrical planetary shadow cast by Saturn onto its rings
    vec3 toFrag = v_worldPosition - u_planetCenter;
    float alongLight = dot(toFrag, L);
    if (alongLight < 0.0) {
        float perpDist = length(toFrag - L * alongLight);
        float shadow = smoothstep(u_planetRadius * 0.95, u_planetRadius * 1.05, perpDist);
        light *= mix(0.06, 1.0, shadow);
    }

    vec3 lit = linearRing * light;
    lit = pow(max(lit, vec3(1e-5)), vec3(1.0 / 2.2));
    float edgeFade = smoothstep(0.0, 0.04, v_texCoord.x) * smoothstep(1.0, 0.96, v_texCoord.x);
    fragColor = vec4(lit, ringColor.a * edgeFade);
}
