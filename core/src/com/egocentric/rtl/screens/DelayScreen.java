package com.egocentric.rtl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.egocentric.rtl.GameController;

public class DelayScreen implements Screen {

	private static final String TAG = "DelayScreen";
	private final GameController gc;
	private FrameBuffer[] frames;



	public static final int TARGET_UPS = 30;
	private static final float SAVE_DELAY = 1f / TARGET_UPS;

	private float drawAccumulator = 0;
	private float saveAccumulator = 0;

	private float drawDelay;

	private int shownFrame = 0;
	private int currentFrame = 0;
	private int targetFrameDelay = 60;
	private int currentFrameDelay = 0;
	private boolean running = false;

	private int maxFrameDelay;
	private boolean debug = false;
	public DelayScreen(GameController gc)
	{
		this.gc = gc;

	}

	public void init(int maxFrameDelay)
	{
		int imgWidth = gc.getImageWidth();
		int imgHeight = gc.getImageHeight();

		this.maxFrameDelay = maxFrameDelay;
		this.shownFrame = -1;
		this.currentFrame = 0;
		this.running = false;
		this.targetFrameDelay = 0;
		this.currentFrameDelay = 0;

		// TODO: Take in as parameter
		drawDelay = SAVE_DELAY;

		if(frames != null)
		{
			// Check if we can use it !
			// is the size adequate
			if(frames.length > (maxFrameDelay + 1))
			{
				// Check the size
				FrameBuffer fbo = frames[0];
				if(fbo.getWidth() == imgWidth && fbo.getHeight() == imgHeight)
				{
					// Everything is super
					return;
				}
			}
			// Dispose of the framebuffer
			for(int i = 0; i < frames.length; i++)
			{
				frames[i].dispose();
				frames[i] = null;
			}
		}
		frames = new FrameBuffer[maxFrameDelay + 2];
		// Make them on the fly instead ?
		for(int i = 0; i < frames.length; i++)
		{
			FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGB888, imgWidth, imgHeight, false);
			frames[i] = fbo;
		}
		// We make sure a picture will be saved at the first run of the render loop.
		saveAccumulator = SAVE_DELAY;
		drawAccumulator = SAVE_DELAY;

	}
	@Override
	public void show()
	{

	}

	@Override
	public void render(float delta)
	{
		saveAccumulator += delta;
		drawAccumulator += delta;


		if(saveAccumulator >= SAVE_DELAY)
		{
			saveAccumulator -= SAVE_DELAY;
			FrameBuffer fbo = frames[currentFrame];
			fbo.begin();
			gc.getCameraController().renderCameraImage();
			fbo.end();
			currentFrame++;
			if(currentFrame >= frames.length)
				currentFrame = 0;
		}

		if(drawAccumulator >= drawDelay)
		{
			drawAccumulator -= drawDelay;
			shownFrame += 1;
			if(shownFrame >= frames.length)
				shownFrame = 0;

			switch(state)
			{

				case SPEED_UP:
				case SLOW_DOWN:
					// We check if were
					calcCurrentFrameDelay();
					if(currentFrameDelay == targetFrameDelay)
					{
						state = State.NORMAL;
						drawDelay = SAVE_DELAY;
						drawAccumulator = saveAccumulator; // To make sure everything is in sync again !
					}
					break;
				/*
					case SLOW_DOWN:
					if(currentFrameDelay <= targetFrameDelay)
					{
						state = State.NORMAL;
						drawDelay = SAVE_DELAY;
					}
					break;
					*/
				case NORMAL:
					// REMOVE
					calcCurrentFrameDelay();
				default:
					// Don't do anything here
					break;
			}
			// Maybe check if needed. if (currentFrameDelay == TargetFrameDelay, no need !
			//calcCurrentFrameDelay();

			/*
			if(currentFrameDelay >= targetFrameDelay)
			{
				drawDelay = SAVE_DELAY;
			}
			*/
		}
		gc.renderAR(frames[shownFrame]);



		if(debug)
		{
			gc.batch.begin();
			gc.font.draw(gc.batch, "ShownFrame: " + shownFrame, 10, Gdx.graphics.getHeight() - 30);
			gc.font.draw(gc.batch, "CurrentFrame: " + currentFrame, 10, Gdx.graphics.getHeight() - 75);
			gc.font.draw(gc.batch, "Delta: " + currentFrameDelay, 10, Gdx.graphics.getHeight() - 120);
			gc.font.draw(gc.batch, String.format("Draw Delay: %.02f", drawDelay), 10, Gdx.graphics.getHeight() - 165);
			gc.font.draw(gc.batch, String.format("Update Delay: %.02f", SAVE_DELAY), 10, Gdx.graphics.getHeight() - 210);
			gc.font.draw(gc.batch, "Target Frame Delay: " + targetFrameDelay, 10, Gdx.graphics.getHeight() - 255);
			gc.font.draw(gc.batch, "State: " + state, 10, Gdx.graphics.getHeight() - 290);
			gc.batch.end();
		}

		// To enable debug
		if(Gdx.input.isTouched())
		{
			if(touched)
			{
				touchAccumulator += delta;
				if(touchAccumulator > 3.0f)
				{
					debug = !debug;
					touchAccumulator = 0.0f;
				}
			}

			touched = true;

		}else
		{
			if(touched)
			{
				touchAccumulator = 0.0f;
				touched = false;
			}
		}
	}

	boolean touched = false;

	float touchAccumulator = 0;

	private void calcCurrentFrameDelay()
	{
		if(shownFrame > currentFrame)
			currentFrameDelay = frames.length - shownFrame + currentFrame;
		else
			currentFrameDelay = currentFrame - shownFrame;
	}

	@Override
	public void resize(int width, int height)
	{

	}

	@Override
	public void pause()
	{

	}

	@Override
	public void resume()
	{

	}

	@Override
	public void hide()
	{

	}

	@Override
	public void dispose()
	{
		if(frames != null)
		{
			for(int i = 0; i < frames.length; i++)
			{
				frames[i].dispose();
				frames[i] = null;
			}
		}
	}

	private enum State {
		SPEED_UP,
		SLOW_DOWN,
		NORMAL,
	}
	private State state = State.NORMAL;
	public void updateFrameDelay(int frameDelay)
	{
		// We will always be one frame behind !
		frameDelay += 1;
		Gdx.app.log(TAG, "");
		calcCurrentFrameDelay();
		this.targetFrameDelay = frameDelay;

		if(targetFrameDelay == 1)
		{
			this.drawDelay = SAVE_DELAY;
			this.state = State.NORMAL;
			this.shownFrame = currentFrame - 1;
			this.drawAccumulator = saveAccumulator;
			return;

		}
		if(currentFrameDelay == frameDelay)
		{ // We're perfect. No need to change anything.
			this.state = State.NORMAL;
			this.drawDelay = SAVE_DELAY;
			Gdx.app.log(TAG, "We're golden !");

		}else if(currentFrameDelay > frameDelay)
		{ // We're to far behind and need to speed up the frames
			this.state = State.SPEED_UP;
			this.drawDelay = SAVE_DELAY * speedUpFactor;
			Gdx.app.log(TAG, "We're to far behind !");
		}else if(currentFrameDelay < frameDelay)
		{
			// We're to far ahead and need to slow down
			this.state = State.SLOW_DOWN;
			this.drawDelay = SAVE_DELAY * slowDownFactor;
			Gdx.app.log(TAG, "We're to fast");

		}

	}

	private static float speedUpFactor = 0.95f;
	private static float slowDownFactor = 1.05f;
}
