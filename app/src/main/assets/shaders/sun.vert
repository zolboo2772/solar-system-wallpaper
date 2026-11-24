#version 320 es
precision highp float;

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;
layout(location = 2) in vec2 a_texCoord;
layout(location = 3) in vec4 a_tangent;

uniform mat4 u_modelMatrix;
uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;

out vec2 v_texCoord;
out vec3 v_normal;
out vec3 v_worldPosition;

void main() {
    vec4 worldPos = u_modelMatrix * vec4(a_position, 1.0);
    v_worldPosition = worldPos.xyz;
    v_texCoord = a_texCoord;
    v_normal = normalize(mat3(u_modelMatrix) * a_normal);
    gl_Position = u_projectionMatrix * u_viewMatrix * worldPos;
}
