package com.egocentric.rtl;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.egocentric.rtl.util.Vector2i;

public interface DeviceCameraControl {

	void init();
	void init(int imgWidth, int imgHeight);
	void init(int imgWidth, int imgHeight, int fps);
	Array<Vector2i> supportedVideoSizes();
	Array<Integer> supportedFPS();
	void renderCameraImage();
	void debugRender(SpriteBatch batch);
	void dispose();


	public boolean isRunning();
	public void stop();
	int getFPS();
	int getImgWidth();
	int getImgHeight();

}
