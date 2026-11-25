#version 320 es
precision highp float;

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;
layout(location = 2) in vec2 a_texCoord;
layout(location = 3) in vec4 a_tangent;

uniform mat4 u_modelMatrix;
uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;
uniform mat3 u_normalMatrix;
uniform vec3 u_cameraPosition;

out vec3 v_worldPosition;
out vec3 v_localPosition;
out vec3 v_normal;
out vec2 v_texCoord;
out vec3 v_viewDir;
out vec3 v_tangent;
out vec3 v_bitangent;

vec3 safeNormalize(vec3 v, vec3 fallback) {
    float d = dot(v, v);
    return d > 1e-8 ? v * inversesqrt(d) : fallback;
}

void main() {
    vec4 worldPos = u_modelMatrix * vec4(a_position, 1.0);
    v_worldPosition = worldPos.xyz;
    v_localPosition = a_position;
    v_texCoord = a_texCoord;

    vec3 N = safeNormalize(u_normalMatrix * a_normal, vec3(0.0, 1.0, 0.0));
    vec3 T = safeNormalize(u_normalMatrix * a_tangent.xyz, vec3(1.0, 0.0, 0.0));
    T = safeNormalize(T - dot(T, N) * N, abs(N.y) < 0.99 ? normalize(cross(vec3(0.0, 1.0, 0.0), N)) : vec3(1.0, 0.0, 0.0));
    vec3 B = cross(N, T) * a_tangent.w;

    v_normal = N;
    v_tangent = T;
    v_bitangent = B;
    v_viewDir = u_cameraPosition - v_worldPosition;

    gl_Position = u_projectionMatrix * u_viewMatrix * worldPos;
}
