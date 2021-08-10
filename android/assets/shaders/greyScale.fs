#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_sampler2D;

void main(){
	gl_FragColor = texture2D(u_sampler2D, v_texCoords) * v_color;
	float y = 0.2126 * gl_FragColor.r + 0.7152 * gl_FragColor.g + 0.0722 * gl_FragColor.b;

	gl_FragColor = vec4(y, y, y, 1.0);
}