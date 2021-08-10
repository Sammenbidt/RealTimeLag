package com.egocentric.rtl.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.egocentric.rtl.DeviceCameraControl;
import com.egocentric.rtl.GameController;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 720;
		config.height = 480;
		DeviceCameraControl controller = new DummyCamera();
		new LwjglApplication(new GameController(controller), config);
	}
}
