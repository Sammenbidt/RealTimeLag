package com.egocentric.rtl;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.egocentric.rtl.util.Vector2i;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AndroidDeviceCameraController implements DeviceCameraControl, Camera.PreviewCallback {

	private static final String TAG = "AndroidCameraController";


	private Camera camera;
	private static byte[]image;
	//private CameraSurface cameraSurface;
	private byte[] pictureData;
	private ByteBuffer yBuffer;
	private ByteBuffer uvBuffer;

	ShaderProgram shader;
	private Texture yTexture;
	private Texture uvTexture;
	Mesh mesh;

	private int imgWidth;
	private int imgHeight;
	private int fps;
	private boolean running = false;
	// END OF TEST
	//private final AndroidLauncher launcher;

	private SurfaceTexture surfaceTexture;
	private static final int DEFAULT_IMG_WIDTH = 1280; // old 1280 x 720
	private static final int DEFAULT_IMG_HEIGHT = 720;
	private static final int DEFAULT_FPS = -1;
	AndroidDeviceCameraController()
	{
		// our YUV image is 12 bits per pixel
		//image = new byte[IMG_WIDTH * IMG_HEIGHT/8 * 12];
	}

	@Override
	public void init()
	{
		this.init(DEFAULT_IMG_WIDTH, DEFAULT_IMG_HEIGHT, DEFAULT_FPS);
	}

	@Override
	public void init(int imgWidth, int imgHeight)
	{
		this.init(imgWidth, imgHeight, DEFAULT_FPS);

	}


	@Override
	public void init(int imgWidth, int imgHeight, int fps)
	{
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
		this.fps = fps;
		running = false;
		image = new byte[imgWidth * imgHeight/8 * 12];
		/*
		 * Initialize the OpenGL/libgdx stuff
		 */

		//Do not enforce power of two texture sizes
		//Texture.setEnforcePotImages(false);

		//Allocate textures
		if(yTexture != null)
		{
			yTexture.dispose();
			yTexture = null;
		}
		yTexture = new Texture(imgWidth,imgHeight, Pixmap.Format.Intensity); //A 8-bit per pixel format
		if(uvTexture != null)
		{
			uvTexture.dispose();
			uvTexture = null;
		}
		uvTexture = new Texture(imgWidth/2,imgHeight/2, Pixmap.Format.LuminanceAlpha); //A 16-bit per pixel format
		//Allocate buffers on the native memory space, not inside the JVM heap
		yBuffer = ByteBuffer.allocateDirect(imgWidth*imgHeight);
		uvBuffer = ByteBuffer.allocateDirect(imgWidth*imgHeight/2); //We have (width/2*height/2) pixels, each pixel is 2 bytes
		yBuffer.order(ByteOrder.nativeOrder());
		uvBuffer.order(ByteOrder.nativeOrder());

		//Our vertex shader code; nothing special
		String vertexShader =
						"attribute vec4 a_position;                         \n" +
						"attribute vec2 a_texCoord;                         \n" +
						"varying vec2 v_texCoord;                           \n" +

						"void main(){                                       \n" +
						"   gl_Position = a_position;                       \n" +
						"   v_texCoord = a_texCoord;                        \n" +
						"}                                                  \n";

		//Our fragment shader code; takes Y,U,V values for each pixel and calculates R,G,B colors,
		//Effectively making YUV to RGB conversion
		String fragmentShader =
						"#ifdef GL_ES                                       \n" +
						"precision highp float;                             \n" +
						"#endif                                             \n" +

						"varying vec2 v_texCoord;                           \n" +
						"uniform sampler2D y_texture;                       \n" +
						"uniform sampler2D uv_texture;                      \n" +

						"void main (void){                                  \n" +
						"   float r, g, b, y, u, v;                         \n" +

						//We had put the Y values of each pixel to the R,G,B components by GL_LUMINANCE,
						//that's why we're pulling it from the R component, we could also use G or B
						"   y = texture2D(y_texture, v_texCoord).r;         \n" +

						//We had put the U and V values of each pixel to the A and R,G,B components of the
						//texture respectively using GL_LUMINANCE_ALPHA. Since U,V bytes are interspread
						//in the texture, this is probably the fastest way to use them in the shader
						"   u = texture2D(uv_texture, v_texCoord).a - 0.5;  \n" +
						"   v = texture2D(uv_texture, v_texCoord).r - 0.5;  \n" +


						//The numbers are just YUV to RGB conversion constants
						"   r = y + 1.13983*v;                              \n" +
						"   g = y - 0.39465*u - 0.58060*v;                  \n" +
						"   b = y + 2.03211*u;                              \n" +

						//We finally set the RGB color of our pixel
						"   gl_FragColor = vec4(r, g, b, 1.0);              \n" +
						"}                                                  \n";

		//Create and compile our shader
		ShaderProgram.pedantic = false;
		shader = new ShaderProgram(vertexShader, fragmentShader);

		if(!shader.isCompiled())
		{
			Gdx.app.log(TAG, "Shader Errors: " + shader.getLog());
		}

		//Create our mesh that we will draw on, it has 4 vertices corresponding to the 4 corners of the screen
		mesh = new Mesh(true, 4, 6,
				new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
				new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"));

		//The vertices include the screen coordinates (between -1.0 and 1.0) and texture coordinates (between 0.0 and 1.0)
		float[] vertices = {
				-1.0f,  1.0f,   // Position 0
				0.0f,   0.0f,   // TexCoord 0
				-1.0f,  -1.0f,  // Position 1
				0.0f,   1.0f,   // TexCoord 1
				1.0f,   -1.0f,  // Position 2
				1.0f,   1.0f,   // TexCoord 2
				1.0f,   1.0f,   // Position 3
				1.0f,   0.0f    // TexCoord 3
		};

		//The indices come in trios of vertex indices that describe the triangles of our mesh
		short[] indices = {0, 1, 2, 0, 2, 3};

		//Set vertices and indices to our mesh
		mesh.setVertices(vertices);
		mesh.setIndices(indices);

		/*
		 * Initialize the Android camera
		 */
		if(camera == null)
			camera = Camera.open(0);
		else
			camera.stopPreview();


		//We set the buffer ourselves that will be used to hold the preview image
		camera.setPreviewCallbackWithBuffer(this);

		//Set the camera parameters
		Camera.Parameters params = camera.getParameters();
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

		// TODO: Add a check if the image size is allowed
		params.setPreviewSize(imgWidth,imgHeight);
		if(fps > 0)
			params.setPreviewFrameRate(fps);

		this.fps = params.getPreviewFrameRate();

		camera.setParameters(params);



		//Start the preview
		camera.startPreview();

		//Set the first buffer, the preview doesn't start unless we set the buffers
		// TODO: What happens if we keep running this

		camera.addCallbackBuffer(image);
		running = true;
		// The surface texture is required for the camera to start preview.
		if(surfaceTexture != null)
		{
			surfaceTexture.release();
			surfaceTexture = null;
		}
		surfaceTexture = new SurfaceTexture(0);
		try
		{
			camera.setPreviewTexture(surfaceTexture);
		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public Array<Integer> supportedFPS()
	{
		if(camera == null)
			camera = Camera.open(0);
		Array<Integer> supportedFps = new Array<>();
		for(Integer i : camera.getParameters().getSupportedPreviewFrameRates())
			supportedFps.add(i);
		return supportedFps;
	}
	@Override
	public Array<Vector2i> supportedVideoSizes()
	{
		if(camera == null)
			camera = Camera.open(0);

		Array<Vector2i> sizes = new Array<>();
		for(Camera.Size size : camera.getParameters().getSupportedPreviewSizes())
		{
			sizes.add( new Vector2i(size.width, size.height));
		}
		return sizes;
	}

	@Override
	public void onPreviewFrame(byte[] bytes, Camera camera)
	{
		if(running)
			camera.addCallbackBuffer(image);
	}

	@Override
	public void renderCameraImage()
	{

		/*
		 * Because of Java's limitations, we can't reference the middle of an array and
		 * we must copy the channels in our byte array into buffers before setting them to textures
		 */

		//Copy the Y channel of the image into its buffer, the first (width*height) bytes are the Y channel

		yBuffer.put(image, 0, imgWidth*imgHeight);
		yBuffer.position(0);


		//Copy the UV channels of the image into their buffer, the following (width*height/2) bytes are the UV channel; the U and V bytes are interspread
		uvBuffer.put(image, imgWidth*imgHeight, imgWidth*imgHeight/2);
		uvBuffer.position(0);

		/*
		 * Prepare the Y channel texture
		 */

		//Set texture slot 0 as active and bind our texture object to it
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		yTexture.bind();

		//Y texture is (width*height) in size and each pixel is one byte; by setting GL_LUMINANCE, OpenGL puts this byte into R,G and B components of the texture
		Gdx.gl.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_LUMINANCE, imgWidth, imgHeight, 0, GL20.GL_LUMINANCE, GL20.GL_UNSIGNED_BYTE, yBuffer);

		// TODO: Add these again, now that the problem is corrected ?
		//Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size

		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);


		/*
		 * Prepare the UV channel texture
		 */

		//Set texture slot 1 as active and bind our texture object to it
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
		uvTexture.bind();

		//UV texture is (width/2*height/2) in size (downsampled by 2 in both dimensions, each pixel corresponds to 4 pixels of the Y channel)
		//and each pixel is two bytes. By setting GL_LUMINANCE_ALPHA, OpenGL puts first byte (V) into R,G and B components and of the texture
		//and the second byte (U) into the A component of the texture. That's why we find U and V at A and R respectively in the fragment shader code.
		//Note that we could have also found V at G or B as well.

		Gdx.gl.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_LUMINANCE_ALPHA, imgWidth/2, imgHeight/2, 0, GL20.GL_LUMINANCE_ALPHA, GL20.GL_UNSIGNED_BYTE, uvBuffer);

		//Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size

		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
		Gdx.gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);


		/*
		 * Draw the textures onto a mesh using our shader
		 */

		shader.begin();
		//Set the uniform y_texture object to the texture at slot 0
		shader.setUniformi("y_texture", 0);
		//Set the uniform uv_texture object to the texture at slot 1
		shader.setUniformi("uv_texture", 1);
		//Render our mesh using the shader, which in turn will use our textures to render their content on the mesh
		mesh.render(shader, GL20.GL_TRIANGLES);
		shader.end();


		// Sets Open GL Texture slot to the first slot.
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

	}

	@Override
	public void debugRender(SpriteBatch batch)
	{
		batch.draw(yTexture,0 ,0);
		batch.draw(uvTexture, yTexture.getWidth(), 0);

	}

	@Override
	public void dispose()
	{
		yTexture.dispose();
		yTexture = null;
		uvTexture.dispose();
		uvTexture = null;
		camera.stopPreview();
		camera.setPreviewCallbackWithBuffer(null);
		camera.release();
		camera = null;
		surfaceTexture.release();
		surfaceTexture = null;
		running = false;
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	@Override
	public void stop()
	{
		if(camera != null)
		{
			camera.stopPreview();
		}
		running = false;
	}

	@Override
	public int getFPS() { return fps; }
	@Override
	public int getImgWidth() { return imgWidth; }
	@Override
	public int getImgHeight() { return imgHeight; }





}
