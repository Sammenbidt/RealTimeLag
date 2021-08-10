package com.egocentric.rtl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.egocentric.rtl.DeviceCameraControl;
import com.egocentric.rtl.GameController;
import com.egocentric.rtl.util.Vector2i;
import com.egocentric.rtl.utils.ShaderController;


public class SetupScreen implements Screen {

	private static final String TAG = "SetupScreen";

	private final GameController gc;
	private Stage stage;
	private OrthographicCamera cam;
	private Viewport viewport;

	private DeviceCameraControl cameraControl;


	private State state = State.CameraSettings;

	private enum State {
		CameraSettings,
		ARSettings,
	}
	public SetupScreen(GameController gc)
	{
		this.gc = gc;

		cam = new OrthographicCamera();
		//cam.setToOrtho(false,640, 480);
		cam.update();
		//stage = new Stage();
		//viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), cam);
		//viewport = new FitViewport(720, 480, cam);
		viewport = new StretchViewport(720, 480, cam);
		viewport.apply();
		//viewport = new ScreenViewport(cam);
		//stage.setViewport(viewport);
		stage = new Stage(viewport);


	}

	private Skin skin;
	private Table root;

	private Label titleLabel;




	// Buttons
	private Button startButton;
	private Button testButton;

	// Tab
	private HorizontalGroup tabGroup;
	private Stack tabStack;
	private Button cameraTab;
	private Button arTab;
	private ButtonGroup<Button> tabButtons;

	private Table cameraTable;
	private Table arTable;

	// Camera UI
	// Resolution UI
	private Table sizeTable;
	private List<Vector2i> sizeList;
	private Label sizeLabel;
	// FPS UI
	private Table fpsTable;
	private List<Integer> fpsList;
	private Label fpsLabel;

	// AR UI
	private Slider offsetSlider;
	private Label offsetLabel;

	// IP
	private Label ipLabel;

	// Shader List
	private List<ShaderController> shaderList;
	//private ShaderProgram shader;
	private ShaderController shader;
	public void initGui(Skin skin)
	{
		this.skin = skin;
		root = new Table(skin);
		root.setFillParent(true);

		DeviceCameraControl cameraController = gc.getCameraController();

		Array<Integer> supportedFps;
		Array<Vector2i> supportedSizes;

		supportedFps = cameraController.supportedFPS();
		supportedSizes = cameraController.supportedVideoSizes();

		/*
			Init the tabs
		 */
		cameraTab = new TextButton("Camera", skin);
		cameraTab.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				setCameraSettingsActive();
				state = State.CameraSettings;
			}
		});

		arTab = new TextButton("AR", skin);
		arTab.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				setARSettingsActive();
				state = State.ARSettings;
			}
		});

		// Init the ButtonGroup for the tabs
		tabButtons = new ButtonGroup<Button>(cameraTab, arTab);
		tabButtons.setMinCheckCount(1);
		tabButtons.setMaxCheckCount(1);

		tabGroup = new HorizontalGroup();
		tabGroup.addActor(cameraTab);
		tabGroup.addActor(arTab);
		root.add(tabGroup).left().padLeft(5).padTop(5).row();


		titleLabel = new Label("Camera Settings", skin);
		root.add(titleLabel).expandX().center().top().padTop(1).colspan(3);
		root.row();




		/*
			Init the AR Tab
		 */
		Label offsetExplain = new Label("Image Offset", skin);
		offsetSlider = new Slider(0, 100, 2, false, skin);
		offsetSlider.setValue(10);

		offsetSlider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				String s = String.format("%03d%s", (int)(offsetSlider.getValue()), "%");
				offsetLabel.setText(s);
				gc.calculateImageOffset(offsetSlider.getValue());
			}
		});
		offsetLabel = new Label("10", skin);
		arTable = new Table(skin);
		arTable.add(offsetExplain).center().top().colspan(3).row();
		arTable.add(offsetSlider).right().top().expand().padRight(4).width(400);
		arTable.add(offsetLabel).left().top().expand().padLeft(4);

		/*
			Init the camera resolution.
		 */
		cameraTable = new Table(skin);
		sizeTable = new Table(skin);
		sizeLabel = new Label("Resolution", skin);
		sizeList = new List<Vector2i>(skin);
		sizeList.setItems(supportedSizes);

		sizeTable.add(sizeLabel).expandX().center().top();
		sizeTable.add( new Label("Shaders", skin)).expandX().top();
		sizeTable.row();
		sizeTable.add(sizeList).expandY().top().center();

		shaderList = new List<ShaderController>(skin);
		shaderList.setItems(gc.shaders);

		sizeTable.add(shaderList).expandY().top().center();

		shaderList.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				if( event instanceof ChangeEvent)
				{
					shader = shaderList.getSelected();
				}
			}
		});


		/*
			End of camera resolution.
		 */

		/*
			Init the camera fps
		 */
		fpsTable = new Table(skin);
		fpsLabel = new Label("FPS", skin);
		fpsList = new List<Integer>(skin);
		fpsList.setItems(supportedFps);

		fpsTable.add(fpsLabel).expandX().center().top();
		fpsTable.row();
		fpsTable.add(fpsList).expandY().top().center();
		/*
			End of init camera fps
		 */

		/*
			Init the test button
		 */
			testButton = new TextButton("Test Settings", skin);
			testButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{
					testCamera();
				}
			});

		/*
			End of init test button
		 */

		/*
			Init the start button
		 */
			startButton = new TextButton("Proceed", skin);
			startButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{
					startCamera();
				}
			});
		/*
			End of init start button
		 */

		/*
			Init the Tab Stack
		 */
		tabStack = new Stack(cameraTable, arTable);

		/*
			Setup the tabs
		 */
		cameraTable.add(sizeTable).left().top().expandX().expandY().padLeft(20);
		cameraTable.add(fpsTable).right().top().expandX().expandY().padRight(20);

		/*
		 Setup the ip label
		 */
		ipLabel = new Label(gc.getIPAddress(), skin);

		root.add(tabStack).colspan(3).expand().fill();
		root.row();
		root.add(testButton).expandX().left().bottom().padBottom(10).padLeft(10);

		root.add(ipLabel).expandX().center().bottom().padBottom(10);

		root.add(startButton).expandX().right().bottom().padBottom(10).padRight(10);

		stage.addActor(root);

		setCameraSettingsActive();

		stage.setDebugAll(false);
	}

	private void testCamera()
	{
		Gdx.app.log(TAG, "TestButton Pressed !");
		Vector2i size = sizeList.getSelected();
		Integer fps = fpsList.getSelected();
		gc.setCameraSettings(size, fps);
		gc.startCamera();
		cameraControl = gc.getCameraController();

	}

	private void setCameraSettingsActive()
	{
		arTable.setVisible(false);
		cameraTable.setVisible(true);
		titleLabel.setText("Camera Settings");
	}

	private void setARSettingsActive()
	{
		cameraTable.setVisible(false);
		arTable.setVisible(true);
		titleLabel.setText("AR Settings");
	}
	private void startCamera()
	{
		Gdx.app.log(TAG, "StartButton Pressed !");

		gc.delayScreen.init(GameController.MAX_FRAME_DELAY);
		//gc.delayScreen.init(30);
		gc.setScreen(gc.delayScreen);


	}
	@Override
	public void show()
	{
		Gdx.input.setInputProcessor(stage);

	}

	@Override
	public void render(float delta)
	{

		cam.update();
		if(Gdx.input.isKeyJustPressed(Input.Keys.D))
		{
			root.setDebug( !root.getDebug(), true);
			stage.setDebugAll(root.getDebug());

		}

		stage.act(delta);

		if(cameraControl != null)
		{
			if(cameraControl.isRunning())
			{
				Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
				switch (state)
				{
					case CameraSettings:
						//gc.renderCamera(gc.waveShader);
						if(shader != null)
							gc.renderCamera(shader);
						else
							cameraControl.renderCameraImage();
						break;
					case ARSettings:
						//gc.renderAR();
						if(shader != null)
							gc.renderAR(shader);
						else
							gc.renderAR();
						break;
				}
			}
		}

		viewport.apply(true);
		stage.draw();



	}


	@Override
	public void resize(int width, int height)
	{
		viewport.update(width, height, true);
		viewport.apply(true);
		//stage.getViewport().update(width, height, true);

		//cam.setToOrtho(false, width, height);
		//cam.update();
		/*
		Viewport viewport = stage.getViewport();
		viewport.update(width, height, true);
		stage.setViewport(viewport);
		*/
		//cam.setToOrtho(false, 640, 480);
		//cam.update();
		//stage.getViewport().update(width, height, true);
		//cam.update();


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

	}
}
