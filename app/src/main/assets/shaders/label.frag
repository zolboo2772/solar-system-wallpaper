#version 320 es
precision mediump float;

in vec2 v_texCoord;

uniform sampler2D u_texture;
uniform float u_alpha;
uniform vec3 u_color;

out vec4 fragColor;

void main() {
    float a = texture(u_texture, v_texCoord).a;
    fragColor = vec4(u_color, a * u_alpha);
}
