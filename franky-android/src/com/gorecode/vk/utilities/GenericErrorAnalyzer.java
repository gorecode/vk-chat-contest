package com.gorecode.vk.utilities;

import java.net.SocketException;
import java.net.UnknownHostException;

import com.gorecode.vk.R;
import com.perm.kate.api.KException;

public class GenericErrorAnalyzer implements ErrorAnalyzer {
	private static final GenericErrorAnalyzer instance = new GenericErrorAnalyzer();

	public static ErrorAnalyzer getInstance() {
		return instance;
	}

	public static boolean isNetworkIssue(Throwable error) {
		return error instanceof SocketException || error instanceof UnknownHostException;
	}

	@Override
	public int getMessageResId(Throwable error) {
		if (isNetworkIssue(error)) {
			return R.string.error_network_issue;
		} else if (error instanceof KException) {
			return R.string.error_internal_error;
		} else {
			return R.string.error_internal_error;
		}
	}
}
