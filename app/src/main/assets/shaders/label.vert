#version 320 es
precision highp float;

layout(location = 0) in vec2 a_position;
layout(location = 1) in vec2 a_texCoord;

uniform vec2 u_screenPosition;
uniform vec2 u_size;
uniform vec2 u_screenSize;

out vec2 v_texCoord;

void main() {
    vec2 pos = a_position * u_size + u_screenPosition;
    vec2 ndc = (pos / u_screenSize) * 2.0 - 1.0;
    ndc.y = -ndc.y;

    v_texCoord = a_texCoord;
    gl_Position = vec4(ndc, 0.0, 1.0);
}
