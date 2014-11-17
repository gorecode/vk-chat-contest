package com.perm.kate.api;

public class LongPollServerInfo {
	public String server;
	public String key;
	public long ts;

	public String toUrl() {
		return String.format("http://%s?act=a_check&key=%s&ts=%d&wait=25&mode=2", server, key, ts);
	}
}
