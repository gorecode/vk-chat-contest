package com.uva.net;

import com.uva.net.ConnectivityManager;

import android.content.Context;
import android.net.NetworkInfo;

public class AndroidConnectivityManager implements ConnectivityManager {
	private final android.net.ConnectivityManager mConnManager;

	public AndroidConnectivityManager(Context context) {
		mConnManager = (android.net.ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public boolean isInternetConnectionPresent() {
		final NetworkInfo[] allNetworks = mConnManager.getAllNetworkInfo();

		for (int i = 0; i < allNetworks.length; i++) {
			final NetworkInfo netInfo = allNetworks[i];

			if (netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
				return true;
			}
		}

		return false;
	}
}
