package com.gorecode.vk.task;

import android.content.Context;

import com.google.inject.Injector;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.data.Profile;
import com.uva.log.Log;

public class GetUserDetailsTask extends LongAction<Long, Profile> {
	private static final String TAG = GetUserDetailsTask.class.getSimpleName();
	
	public GetUserDetailsTask(Context context) {
		this(context, null);
	}
	
	public GetUserDetailsTask(Context context, Profile person) {
		super(context);

		wrapWithProgress(false);
	}
	
	@Override
	protected Profile doInBackgroundOrThrow(Long params) throws Exception {
		Log.debug(TAG, "Getting profile information.");

		Injector injector = VkApplication.from(getContext()).getInjector();
		
		//RemoteSearchGateway remoteSearchGateway = injector.getInstance(RemoteSearchGateway.class); 

		long profileId = params;
		
		// TODO:
		Profile result = null;

		Log.debug(TAG, "Profile information is recived.");

		return result;
	}
	
	@Override
	protected void onError(Exception error) {		
		super.onError(error);
		Log.exception(TAG, "Unable to load profile information", error);
	}
}
