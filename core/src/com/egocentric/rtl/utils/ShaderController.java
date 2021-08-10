package com.egocentric.rtl.utils;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public abstract class ShaderController {

	protected final ShaderProgram shader;
	protected final String name;
	public ShaderController(ShaderProgram shader, String name)
	{
		this.shader = shader;
		this.name = name;

	}
	public ShaderController(String name, ShaderProgram shader) { this(shader, name);}


	public void update(float delta)
	{

	}

	public String getName() { return name; }
	public ShaderProgram getShader() { return shader; }

	@Override
	public String toString() { return name; }
}
