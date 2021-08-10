package com.egocentric.rtl;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.egocentric.rtl.screens.DelayScreen;
import com.egocentric.rtl.screens.SetupScreen;
import com.egocentric.rtl.server.RTLControllerServer;
import com.egocentric.rtl.util.Vector2i;
import com.egocentric.rtl.utils.ShaderController;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class GameController extends Game {

	private static final String TAG = "GameController";
	private final int DEFAULT_IMG_WIDTH  = 640; // 640
	private final int DEFAULT_IMG_HEIGHT = 480; // 480
	public static final int MAX_FRAME_DELAY = 5 * DelayScreen.TARGET_UPS; // 5 seconds = 30 * 5


	public SpriteBatch batch;
	public BitmapFont font;

	private FrameBuffer fbo;
	// Not used !
	private TextureRegion fboRegion;



	public Skin skin;

	private int imageOffset = 20;

	private int imageWidth = -1;
	private int imageHeight = -1;
	private int fps = -1;

	private final DeviceCameraControl deviceCameraControl;

	public SetupScreen setupScreen;
	public DelayScreen delayScreen;

	public AssetManager assetManager;

	public GameController(DeviceCameraControl cameraController)
	{
		super();
		this.deviceCameraControl = cameraController;


	}

	
	@Override
	public void create () {
		batch = new SpriteBatch();

		assetManager = new AssetManager();
		assetManager.load("skin/uiskin.json", Skin.class);
		assetManager.update();
		assetManager.finishLoading();
		skin = assetManager.get("skin/uiskin.json", Skin.class);

		font = new BitmapFont();
		font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		font.getData().setScale(3,3);

		this.setupWebServer();

		imageWidth = DEFAULT_IMG_WIDTH;
		imageHeight = DEFAULT_IMG_HEIGHT;
		initShaders();
		setupScreen = new SetupScreen(this);
		setupScreen.initGui(skin);
		delayScreen = new DelayScreen(this);


		this.setScreen(setupScreen);

		//this.deviceCameraControl.init(640, 480, 15);
		//fbo = new FrameBuffer(Pixmap.Format.RGB888, 640, 480, false);

		// INIT THE Shaders!



	}

	// TESTING
	RTLControllerServer server;
	private void setupWebServer()
	{
		Gdx.app.log(TAG, "Setting up the webserver");
		server = new RTLControllerServer(this, 8080);


		try
		{
			server.start();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		//RTLControllerServer testServer = new RTLControllerServer(8080);



	}

	public void calculateImageOffset(float percentage)
	{
		if(percentage < 0 )
			percentage = 0;
		if(percentage > 100)
			percentage = 100;
		imageOffset =(int) (percentage/100f * imageWidth);
	}


	@Override
	public void render () {
		Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT |GL20.GL_DEPTH_BUFFER_BIT);

		//oldRender();
		super.render();

	}

	@Override
	public void dispose()
	{
		super.dispose();

		if(deviceCameraControl != null)
		{
			deviceCameraControl.dispose();
		}
		if(setupScreen != null) setupScreen.dispose();
		if(delayScreen != null) delayScreen.dispose();
		if(skin != null) skin.dispose();

		if(server != null)
		{
			server.stop();
			server = null;
		}
		for(ShaderController shaderContainer : shaders)
		{
			if(shaderContainer.getShader() != null)
			{
				shaderContainer.getShader().dispose();
			}
		}
		shaders.clear();
		if(fbo != null)
			fbo.dispose();
		if(fbo2 != null)
			fbo2.dispose();

	}

	public DeviceCameraControl getCameraController() { return deviceCameraControl; }

	public void setCameraSettings(Vector2i size, Integer fps)
	{
		this.setCameraSettings(size.x, size.y, fps);
	}
	public void setCameraSettings(int imageWidth, int imageHeight, int fps)
	{
		arReady = false;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.fps = fps;
	}

	public void startCamera()
	{
		if(fps > 0)
		{
			if(imageWidth > 0 && imageHeight > 0)
				deviceCameraControl.init(imageWidth, imageHeight, fps);
			else
				deviceCameraControl.init();
		}else
		{
			if(imageWidth > 0 && imageHeight > 0)
				deviceCameraControl.init(imageWidth, imageHeight);
			else
				deviceCameraControl.init();

		}
	}

	private boolean arReady = false;
	public void setupAR()
	{
		if(fbo != null)
		{
			// Check if the dimensions match
			if(fbo.getWidth() == imageWidth && fbo.getHeight() == imageHeight)
			{
				arReady = true;
				return;
			}

			fbo.dispose();
			fbo = null;
		}

		fbo = new FrameBuffer(Pixmap.Format.RGB888, imageWidth, imageHeight, false);
		arReady = true;
	}

	public void renderAR(FrameBuffer fbo, boolean flipHorizontal, boolean flipVertical)
	{
		batch.begin();
		batch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight(), 0, 0, fbo.getWidth() - imageOffset, fbo.getHeight(), flipHorizontal, !flipVertical);
		batch.draw(fbo.getColorBufferTexture(), Gdx.graphics.getWidth()/2, 0, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight(), imageOffset, 0, fbo.getWidth() - imageOffset, fbo.getHeight(), flipHorizontal, !flipVertical);
		batch.end();
	}

	public void renderAR(FrameBuffer fbo)
	{
		this.renderAR(fbo, flipHorizontal, flipVertical);
		/*
		batch.begin();

		batch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight(), 0, 0, fbo.getWidth() - imageOffset, fbo.getHeight(), false, true);
		batch.draw(fbo.getColorBufferTexture(), Gdx.graphics.getWidth()/2, 0, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight(), imageOffset, 0, fbo.getWidth() - imageOffset, fbo.getHeight(), false, true);

		batch.end();
		*/
	}
	FrameBuffer fbo2;

	private FrameBuffer checkFrameBuffer(FrameBuffer frame)
	{
		if(frame!= null)
		{
			if(frame.getWidth() != imageWidth || frame.getHeight() != imageHeight )
			{
				frame.dispose();
				frame = null;
			}
		}
		if(frame == null)
			frame = new FrameBuffer(Pixmap.Format.RGB888, imageWidth, imageHeight, false);
		return frame;
	}
	public void renderAR(ShaderController shader)
	{
		if(!arReady)
			setupAR();


		fbo.begin(); // Render the original image !
		deviceCameraControl.renderCameraImage();
		fbo.end();

		fbo2 = checkFrameBuffer(fbo2);
		fbo2.begin();
		batch.begin();
		batch.setShader(shader.getShader());
		if(shader.getShader() != null)
		{
			shader.update(Gdx.graphics.getDeltaTime());

		}
			// WE're using a viewport that uses the size of our graphic device...

			batch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, fbo.getWidth(), fbo.getHeight(), false, true);
		batch.end();
		batch.setShader(null);
		fbo2.end();

		renderAR(fbo2);
	}



	public void renderCamera(ShaderController shader)
	{
		fbo = checkFrameBuffer(fbo);
		renderCamera(fbo);
		//renderFrameBuffer(fbo2);

		Gdx.gl.glViewport(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.setShader(shader.getShader());
		/*
		if(shader.getShader() != null)
		{
			Gdx.app.log(TAG, " Updating shader !");

			shader.getShader().begin();

			//shader.update(Gdx.graphics.getDeltaTime());

		}
		*/


		batch.begin();

			batch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, fbo.getWidth(), fbo.getHeight(), false, true);
		batch.end();

		batch.setShader(null);

	}
	public void renderFrameBuffer(FrameBuffer frame)
	{
		batch.begin();
		batch.draw(frame.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, frame.getWidth(), frame.getHeight(), false, true);
		batch.end();
	}

	public void renderCamera(FrameBuffer target)
	{
		// Render the camera image to the FBO
		Gdx.gl.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
		target.bind();
		deviceCameraControl.renderCameraImage();
		target.end();
	}

	public void renderCamera(ShaderController shader, FrameBuffer target)
	{
		// First we render the camera image to the fbo !
		if(fbo == target)
			Gdx.app.error(TAG, "Error in renderCamera, target shouldn't be fbo variable");

		fbo = checkFrameBuffer(fbo);
		renderCamera(fbo);

		//Gdx.app.log(TAG, "Target : [ " + target.getWidth() + " x " + target.getHeight() + " ]");
		//Gdx.app.log(TAG, "fbo : [ " + fbo.getWidth() + " x " + fbo.getHeight() + " ]");
		// I'm assuming the target has the same size as the fbo
		//Gdx.gl.glViewport(0, 0, target.getWidth(), target.getHeight());
		target.begin();
		batch.setShader(shader.getShader());
		/*
		if(shader.getShader() != null)
			shader.update(Gdx.graphics.getDeltaTime());
		*/
		batch.begin();
			batch.draw(fbo.getColorBufferTexture(),0 ,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, fbo.getWidth(), fbo.getHeight(), false, false);
		batch.end();

		batch.setShader(null);
		target.end();
	}
	public void renderAR()
	{
		if(!arReady)
			setupAR();

		fbo.begin();
		deviceCameraControl.renderCameraImage();
		fbo.end();
		renderAR(fbo);
	}

	public int getImageHeight()
	{
		return imageHeight;
	}

	public int getImageWidth()
	{
		return imageWidth;
	}


	private int frameDelay = 0;
	private boolean flipHorizontal = false;
	private boolean flipVertical = false;
	public void updateDelay(int frameDelay, boolean flipHorizontal, boolean flipVertical)
	{
		if(frameDelay >= 0 && frameDelay <= MAX_FRAME_DELAY)
		{
			this.frameDelay = frameDelay;
			delayScreen.updateFrameDelay(this.frameDelay);
		}
		else
			Gdx.app.log(TAG, "Unavailable frame delay chosen: " + frameDelay + ". Must be between [0 -  " + MAX_FRAME_DELAY + "]" );
		this.flipHorizontal = flipHorizontal;
		this.flipVertical = flipVertical;
	}
	public int getFrameDelay() { return frameDelay; }
	public boolean getFlipHorizontal() {return flipHorizontal;}
	public boolean getFlipVertical() { return flipVertical; }


	public String getIPAddress()
	{
		if(server != null)
		{
			List<String> addresses = new ArrayList<String>();
			try {
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				for(NetworkInterface ni : Collections.list(interfaces)){
					for(InetAddress address : Collections.list(ni.getInetAddresses()))
					{
						if(address instanceof Inet4Address){
							addresses.add(address.getHostAddress());
						}
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
			}
			//Gdx.app.log(TAG, "Number of addresses:" + addresses.size());
			String hostName = "No IP";
			for(String s : addresses)
			{
				if(s.contains("127.0.0.1"))
					continue;
				hostName = s;
				//Gdx.app.log(TAG, "Address: " + s);
			}


			StringBuilder sb = new StringBuilder();
			/*
			byte[] address = new byte[0];
			try
			{
				address = InetAddress.getLocalHost().getAddress();
			} catch (UnknownHostException e)
			{
				e.printStackTrace();
			}
			for(int i = 0; i < address.length; i++)
			{
				if ( i != 0)
					sb.append(".");
				int b = address[i] & 0xFF;
				sb.append(b);
			}
			*/
			//String hostName = sb.toString();
			return hostName + ":" + server.getListeningPort();
		}
		return "Server not started.";
	}


	public Array<ShaderController> shaders = new Array<ShaderController>();
	public ShaderProgram greyScaleShader;
	public ShaderProgram waveShader;
	public ShaderProgram bloomShader;


	private ShaderProgram compileShader(String name, String vertex, String fragment)
	{
		ShaderProgram program = new ShaderProgram(vertex, fragment);

		if(program.isCompiled())
		{
			Gdx.app.log(TAG, "Shader: " + name + " compiled successfully!");
			shaders.add(new ShaderController(program, name) {
			});
			return program;
		}else
		{
			Gdx.app.error(TAG, "Error when compiling Shader: ");
			Gdx.app.error(TAG, program.getLog());

		}
		return null;
	}
	private void initShaders()
	{

		ShaderProgram.pedantic = false;
		// Create the empty shader
		shaders.add(new ShaderController(null, "None") {
		});
		compileShader("GreyScale",
				Gdx.files.internal("shaders/greyScale.vs").readString(),
				Gdx.files.internal("shaders/greyScale.fs").readString()
		);
		compileShader("Wave",
				Gdx.files.internal("shaders/wave.vs").readString(),
				Gdx.files.internal("shaders/wave.fs").readString()
		);
		compileShader("Blur",
				Gdx.files.internal("shaders/blur.vs").readString(),
				Gdx.files.internal("shaders/blur.fs").readString()
		);

		String standingVertex = Gdx.files.internal("shaders/standingWave.vs").readString();
		String standingFragment = Gdx.files.internal("shaders/standingWave.fs").readString();
		ShaderProgram standingWaveShader = new ShaderProgram(standingVertex, standingFragment);
		if(standingWaveShader.isCompiled())
		{
			ShaderController standingShaderController = new ShaderController("Standing Wave", standingWaveShader) {
				float time = 0;
				@Override
				public void update(float delta)
				{
					time += delta * 10f;
					// We assume we've bound the shader here


					shader.setUniformf("time", delta);

					Gdx.app.log("StandingWave", "Time : " + time);

				}
			};
			shaders.add(standingShaderController);
		}

		/*
		compileShader("StandingWave",
				Gdx.files.internal("shaders/standingWave.vs").readString(),
				Gdx.files.internal("shaders/standingWave.fs").readString()
		);
		*/

		/*
		String greyScaleVertex = Gdx.files.internal("shaders/greyScale.vs").readString();
		String greyScaleFragment = Gdx.files.internal("shaders/greyScale.fs").readString();

		greyScaleShader = new ShaderProgram(greyScaleVertex, greyScaleFragment);

		if(!greyScaleShader.isCompiled())
		{
			Gdx.app.error(TAG, "Error when compiling greyscale shader !");
			Gdx.app.error(TAG, greyScaleShader.getLog());
			greyScaleShader.dispose();
			greyScaleShader = null;

			//return;
		}else{
			shaders.add(greyScaleShader);

		}

		String waveVertex = Gdx.files.internal("shaders/wave.vs").readString();
		String waveFragment = Gdx.files.internal("shaders/wave.fs").readString();

		waveShader = new ShaderProgram(waveVertex, waveFragment);

		if(!waveShader.isCompiled())
		{
			Gdx.app.error(TAG, "Error when compiling wave shader !");
			Gdx.app.error(TAG, waveShader.getLog());
			waveShader.dispose();
			waveShader = null;

			//return;
		}

		String bloomVertex = Gdx.files.internal("shaders/bloom.vs").readString();
		String bloomFragment = Gdx.files.internal("shaders/bloom.fs").readString();

		bloomShader = new ShaderProgram(bloomVertex, bloomFragment);

		if(!bloomShader.isCompiled())
		{
			Gdx.app.error(TAG, "Error when compiling bloom shader !");
			Gdx.app.error(TAG, bloomShader.getLog());
			bloomShader.dispose();
			bloomShader = null;

			//return;
		}

		*/
		Gdx.app.log(TAG, "Successfully compiled shaders!");


	}

	private ShaderController activeShader;
	public void setActiveShader(ShaderController shaderController)
	{
		this.activeShader = shaderController;
	}

}
