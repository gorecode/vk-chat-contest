package com.gorecode.vk.data;

public class Size {
	public Size() {
		;
	}

	public Size(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return width + "x" + height + ";"; 
	}

	public int width;
	public int height;
}
