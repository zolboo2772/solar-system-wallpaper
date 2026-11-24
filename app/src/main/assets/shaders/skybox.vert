#version 320 es
precision highp float;

layout(location = 0) in vec3 a_position;

uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;

out vec3 v_texCoord;

void main() {
    v_texCoord = a_position;
    mat4 viewNoTranslation = mat4(mat3(u_viewMatrix));
    vec4 pos = u_projectionMatrix * viewNoTranslation * vec4(a_position, 1.0);
    gl_Position = pos.xyww;
}
