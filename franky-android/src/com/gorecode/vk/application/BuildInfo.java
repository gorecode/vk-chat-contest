package com.gorecode.vk.application;

// Warning: this class is rewritten on every build though ant. 
public class BuildInfo {
	public static final boolean IS_DEBUG_BUILD = true;

	public static final String BUILD_NUMBER = "buildnum";
	public static final String BUILD_REVISION = "buildrev";
	public static final String BUILD_DATE = "builddate";

	private BuildInfo() {
		;
	}
}
