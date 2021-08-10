#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_sampler2D;
//uniform float tx, ty;
float delta = 3.0;
void main()
{
	vec4 sum = vec4(0.0, 0.0, 0.0, 1.0);
	// Spread !
	
	float x = -delta;
	float y = -delta;
	for(x = -delta ; x <= delta; x++)
	{
		for( y = -delta; y <= delta; y++)
		{
			sum += texture2D(u_sampler2D, v_texCoords + vec2(x/640.0, y/480.0));	
		}

		
	}
	sum /= (delta * 2.0  + 1.0) * ( delta * 2.0 + 1.0) ;
	sum.a = 1.0;

	
	gl_FragColor = sum;
	//gl_FragColor = texture2D(u_sampler2D, SineWave(v_texCoords));
}