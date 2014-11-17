package com.gorecode.vk.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.google.common.base.Function;
import com.gorecode.vk.api.VkModel.AuthResult;
import com.gorecode.vk.data.Profile;
import com.uva.io.FileStorage;
import com.uva.log.Log;
import com.uva.utilities.ObserverCollection;

public class SessionContext implements Serializable {
	private static final long serialVersionUID = -8836388783471781488L;

	private static final String SAVED_INSTANCE_STATE_FILENAME = SessionContext.class.getSimpleName() + ".state";

	public static final String TAG = "SessionContext";

	public static interface OnUnhandledNotificationsUpdateListener {
		public void onUnhandledNotificationsUpdate();
	};

	transient private ObserverCollection<OnUnhandledNotificationsUpdateListener> onUnhandledNotificationsUpdateListeners;

	AuthResult authResult;

	Profile user;

	public SessionContext() {
		initPostDeserialize();
	}

	static SessionContext restoreOrCreate(FileStorage fs) {
		SessionContext context = restore(fs);
		if (context == null) {
			context = new SessionContext();
		}
		return context;
	}

	static SessionContext restore(FileStorage fs) {
		try {
			if (!fs.isFileExists(SAVED_INSTANCE_STATE_FILENAME)) return null;

			InputStream fileInput = fs.openFileInput(SAVED_INSTANCE_STATE_FILENAME);

			try {
				SessionContext object = (SessionContext)new ObjectInputStream(fileInput).readObject();
				object.initPostDeserialize();
				return object;
			} finally {
				try {
					fileInput.close();
				} catch (IOException e) {
					;
				}
			}
		} catch (Exception e) {
			Log.exception(TAG, "Unable to load SessionContext from file", e);
			return null;
		}
	}

	public String getSecret() {
		if (authResult == null) {
			return null;
		}
		return authResult.secret;
	}

	public String getAccessToken() {
		if (authResult == null) {
			return null;
		}
		return authResult.accessToken;
	}

	public String getAccessTokenForHttps() {
		if (authResult == null) {
			return null;
		}
		return authResult.accessTokenForHttps;
	}

	public boolean isUserAuthorized() {
		return authResult != null;
	}

	public Profile getUser() {
		return user;
	}

	public long getUserId() {
		return user.id;
	}

	public void addOnUnhandledNotificationsUpdateListener(OnUnhandledNotificationsUpdateListener listener) {
		onUnhandledNotificationsUpdateListeners.add(listener);
	}

	public void removeOnUnhandledNotificationsUpdateListener(OnUnhandledNotificationsUpdateListener listener) {
		onUnhandledNotificationsUpdateListeners.remove(listener);
	}

	boolean saveInstanceState(FileStorage fs) {
		try {
			OutputStream fileOut = fs.openFileOutput(SAVED_INSTANCE_STATE_FILENAME);

			try {
				new ObjectOutputStream(fileOut).writeObject(this);
			} finally {
				fileOut.close();
			}
		} catch (IOException e) {
			Log.exception(TAG, "Unable to save instance state", e);
			return false;
		}
		return true;
	}

	public void notifyUnhandledNotificationsUpdated() {
		onUnhandledNotificationsUpdateListeners.callForEach(new Function<SessionContext.OnUnhandledNotificationsUpdateListener, Void>() {
			@Override
			public Void apply(OnUnhandledNotificationsUpdateListener arg) {
				arg.onUnhandledNotificationsUpdate();

				return null;
			}
		});
	}

	void initPostDeserialize() {
		onUnhandledNotificationsUpdateListeners = new ObserverCollection<OnUnhandledNotificationsUpdateListener>();
	}
}
