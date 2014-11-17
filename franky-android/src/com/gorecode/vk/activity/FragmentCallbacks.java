package com.gorecode.vk.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public interface FragmentCallbacks {
	public void onFragmentCreated(Fragment fragment, Bundle savedInstanceState);
	public void onFragmentViewCreated(Fragment fragment, View view, Bundle savedInstanceState);
	public void onFragmentViewDestroyed(Fragment fragment);
	public void onFragmentDestroyed(Fragment fragment);
}
