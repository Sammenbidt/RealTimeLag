package com.egocentric.rtl;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
	private static final String TAG = "AndroidLauncher";

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

		config.r = 8;
		config.g = 8;
		config.b = 8;
		config.a = 8;


		DeviceCameraControl cameraControl = new AndroidDeviceCameraController();
		initialize(new GameController(cameraControl), config);

		graphics.getView().setKeepScreenOn(true);

	}

}
