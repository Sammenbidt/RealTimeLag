package com.egocentric.rtl.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.egocentric.rtl.DeviceCameraControl;
import com.egocentric.rtl.util.Vector2i;

public class DummyCamera implements DeviceCameraControl {
	private static final String TAG = "DummyCamera";
	private int imageWidth = -1, imageHeight = -1;
	private int fps = -1;

	private Texture texture;
	private SpriteBatch batch;
	@Override
	public void init()
	{
		texture = new Texture("cameraTexture.jpg");
		batch = new SpriteBatch();
		Gdx.app.log(TAG, "INIT()");

	}

	@Override
	public void init(int imgWidth, int imgHeight)
	{
		this.init(imgWidth, imgHeight, fps);

	}


	@Override
	public void init(int imgWidth, int imgHeight, int fps)
	{
		this.imageWidth = imgWidth;
		this.imageHeight = imgHeight;
		this.fps = fps;
	}

	@Override
	public Array<Vector2i> supportedVideoSizes()
	{
		return new Array(new Vector2i[]{new Vector2i(640, 480), new Vector2i(720, 480), new Vector2i(720, 576), new Vector2i(1280,720), new Vector2i(1920, 1080)});
	}

	@Override
	public Array<Integer> supportedFPS()
	{
		return new Array<Integer>( new Integer[] {5, 10, 15, 20, 25, 30} );
	}

	@Override
	public void renderCameraImage()
	{
		if(batch == null)
			init();
		batch.begin();
		batch.draw(texture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		batch.end();
	}

	@Override
	public void debugRender(SpriteBatch batch)
	{
		batch.draw(texture, 0, 0);

	}

	@Override
	public void dispose()
	{
		if(batch != null)batch.dispose();
		if(texture != null) texture.dispose();

	}

	@Override
	public boolean isRunning()
	{
		return true;
	}

	@Override
	public void stop()
	{

	}

	@Override
	public int getFPS()
	{
		return fps;
	}

	@Override
	public int getImgWidth()
	{
		return imageWidth;
	}

	@Override
	public int getImgHeight()
	{
		return imageHeight;
	}
}
