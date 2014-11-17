package com.gorecode.vk.imageloader;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class FailReason {
	public final Exception what;

	public FailReason(Exception what) {
		this.what = what;
	}
}
