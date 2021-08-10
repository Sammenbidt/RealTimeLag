#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

float frequency = 8.0;
float amplitude = 0.1;

uniform sampler2D u_sampler2D;

//uniform float tx, ty;

uniform float time;

void main()
{

	vec2 pulse = sin(time - frequency * v_texCoords);
	vec2 coord = v_texCoords + amplitude * vec2(pulse.x, -pulse.x);
	gl_FragColor = texture2D(u_sampler2D, coord);
	//gl_FragColor = texture2D(u_sampler2D, SineWave(v_texCoords));
}