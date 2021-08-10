package com.egocentric.rtl.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.egocentric.rtl.GameController;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class RTLControllerServer extends NanoHTTPD {


	private static final String TAG = "RTLControllerServer";
	private final GameController gc;
	private int port;
	private String html;
	private boolean running;

	public RTLControllerServer(GameController gc)
	{
		this(gc,8080);
	}
	public RTLControllerServer(GameController gc, int port)
	{
		super(port);
		this.gc = gc;
		this.port = port;



		//File file = new File("controller.html");
		//Gdx.files.internal("controller.html");

		FileHandle handle = Gdx.files.internal("controller.html");
		html = handle.readString();
		html = html.replace("{{MAX_FRAME_DELAY}}", GameController.MAX_FRAME_DELAY + "");

		/*
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			while( (line = reader.readLine()) != null)
			{
				// TODO: Add changes to file here. The maximum number of frames skipped.
				if(line.contains("{{MAX_FRAME_SKIP}}"))
					line = line.replace("{{MAX_FRAME_DELAY}}", "90");
				sb.append(line);
				sb.append("\n");
			}
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		*/
		//html = sb.toString();



	}

	private String getHtml()
	{
		int frameDelay = gc.getFrameDelay();
		boolean flipHorizontal = gc.getFlipHorizontal();
		boolean flipVertical = gc.getFlipVertical();
		String s = html.replace("{{FRAME_DELAY}}", frameDelay + "");
		s = s.replace("{{MAX_FRAME_DELAY}}", GameController.MAX_FRAME_DELAY + "");
		s = s.replace("{{SEC_DELAY}}", String.format("%.2f sec", frameDelay / 30.0f)); // Change to a variable ?
		s = s.replace("{{FLIP_HORIZONTAL}}", flipHorizontal ? "checked" : "");
		s = s.replace("{{FLIP_VERTICAL}}", flipVertical ? "checked" : "");

		return s;
	}
	/*
	private boolean flipHorizontal = true;
	private boolean flipVertical = false;
	private int frameDelay = 72;
	*/
	@Override
	public Response serve(IHTTPSession session)
	{
		String uri = session.getUri();
		Gdx.app.log(TAG, "URI : " + uri);

		if(uri.contains("/3update"))
		{

			Response response = new Response(Response.Status.REDIRECT, MIME_HTML, "");
			response.addHeader("Location", "http://google.com");
		}
		if(session.getMethod() == Method.POST)
		{
			try
			{
				session.parseBody(new HashMap<String, String>());
				Map<String,String> params = session.getParms();
				String strFrameDelay = params.get("frame_delay");
				String strFlipHori = params.get("flip_horizontal");
				String strFlipVerti = params.get("flip_vertical");
				int frameDelay = gc.getFrameDelay();
				if(strFrameDelay != null)
				{
					frameDelay = Integer.parseInt(strFrameDelay);
				}
				boolean flipHorizontal = strFlipHori != null;
				boolean flipVertical = strFlipVerti != null;

				gc.updateDelay(frameDelay, flipHorizontal, flipVertical);
			} catch (IOException e)
			{
				e.printStackTrace();
			} catch (ResponseException e)
			{
				e.printStackTrace();
			}

			/*
			Response response = new Response(Response.Status.REDIRECT, MIME_HTML, getHtml());

			//response.addHeader("Location", "");
			return response;
			*/
		}


		//String s = getHtml();
		Response response = new Response(Response.Status.OK, MIME_HTML, getHtml());

		return response;
	}

	@Override
	public void start() throws IOException
	{
		super.start();

		running = false;
	}

	public boolean isRunning() { return running; }


}
