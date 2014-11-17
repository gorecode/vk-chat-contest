package com.gorecode.vk.imageloader;

/**
 * Present width and height values
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class ImageSize {
	final int width;
	final int height;

	public ImageSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(6);
		builder.append(width);
		builder.append('x');
		builder.append(height);
		return builder.toString();
	}
}
