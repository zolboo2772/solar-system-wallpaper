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
out vec3 v_normal;
out vec2 v_texCoord;
out vec3 v_viewDir;
out vec3 v_tangent;
out vec3 v_bitangent;

void main() {
    vec4 worldPos = u_modelMatrix * vec4(a_position, 1.0);
    v_worldPosition = worldPos.xyz;
    v_texCoord = a_texCoord;

    vec3 N = normalize(u_normalMatrix * a_normal);
    vec3 T = normalize(u_normalMatrix * a_tangent.xyz);
    T = normalize(T - dot(T, N) * N);
    vec3 B = cross(N, T) * a_tangent.w;

    v_normal = N;
    v_tangent = T;
    v_bitangent = B;
    v_viewDir = u_cameraPosition - v_worldPosition;

    gl_Position = u_projectionMatrix * u_viewMatrix * worldPos;
}
