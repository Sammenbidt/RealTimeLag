package com.egocentric.rtl.util;

public class Vector2i {
	public int x;
	public int y;

	public Vector2i()
	{
		this(0,0);
	}

	public Vector2i(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public void set(Vector2i other)
	{
		this.set(other.x, other.y);
	}
	public void set(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString()
	{
		return String.format("[%d x %d]", x, y);
	}
}
