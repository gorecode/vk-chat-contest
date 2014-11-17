package com.gorecode.vk.config;

import java.io.IOException;

public interface ConfigStorage {
	public void restoreConfigState(Config config) throws IOException;
	public void saveConfigState(Config config) throws IOException;
}
