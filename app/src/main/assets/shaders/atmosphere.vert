#version 320 es
precision highp float;

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;

uniform mat4 u_modelMatrix;
uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;
uniform vec3 u_cameraPosition;

out vec3 v_worldNormal;
out vec3 v_viewDir;

void main() {
    vec4 worldPos = u_modelMatrix * vec4(a_position, 1.0);
    float d = dot(a_normal, a_normal);
    vec3 safeN = d > 1e-8 ? a_normal * inversesqrt(d) : vec3(0.0, 1.0, 0.0);
    v_worldNormal = normalize(mat3(u_modelMatrix) * safeN);
    v_viewDir = u_cameraPosition - worldPos.xyz;
    gl_Position = u_projectionMatrix * u_viewMatrix * worldPos;
}
