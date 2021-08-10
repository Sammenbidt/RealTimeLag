#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_sampler2D;

//uniform float tx, ty;

float tx = 0.0; // OLD : 0.3477
float ty = 0.0; // OLD : 0.7812
vec2 SineWave( vec2 p)
{
	// convert Vertex position <-1, 1> to texture coordinates <0, 1> and some shrinking so the effect dont overlap screen
	//p.x = ( 0.55 * p.x) + 0.5;
	//p.y = (-0.55 * p.y) + 0.5;
	// wave distortion
	float x = sin( 25.0 * p.y + 30.0 * p.x + 6.28 * tx) * 0.05;
	float y = sin( 25.0 * p.y + 30.0 * p.x + 6.28 * ty) * 0.05;
	return vec2(p.x + x, p.y + y);
}

void main()
{
	gl_FragColor = texture2D(u_sampler2D, SineWave(v_texCoords));
}