package com.gorecode.vk.utilities;

import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;

import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.FastPackUnpack;
import com.gorecode.vk.data.Photo;
import com.gorecode.vk.data.Profile;
import com.uva.location.Location;

public class BundleUtilities {
	public static void putExtra(Intent intent, String name, Dialog dialog) {
		try {
			intent.putExtra(name, FastPackUnpack.serializeDialog(dialog));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static void putExtra(Intent intent, String name, ChatMessage message) {
		try {
			intent.putExtra(name, FastPackUnpack.serializeChatMessage(message));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static void putExtra(Intent intent, String name, Photo photo) {
		try {
			intent.putExtra(name, FastPackUnpack.serializePhoto(photo));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static void putExtra(Intent intent, String name, Profile profile) {
		try {
			intent.putExtra(name, FastPackUnpack.serializeProfile(profile));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static void putExtra(Intent intent, String name, Location location) {
		try {
			intent.putExtra(name, FastPackUnpack.serializeLocation(location));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static ChatMessage getChatMessage(Bundle bundle, String name) {
		if (bundle == null || !bundle.containsKey(name)) return null;

		try {
			return FastPackUnpack.deserializeChatMessage(bundle.getByteArray(name));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static Dialog getDialog(Bundle bundle, String name) {
		if (bundle == null || !bundle.containsKey(name)) return null;

		try { 
			return FastPackUnpack.deserializeDialog(bundle.getByteArray(name));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}

	}

	public static Photo getPhoto(Bundle bundle, String name) {
		if (bundle == null || !bundle.containsKey(name)) return null;

		try {
			return FastPackUnpack.deserializePhoto(bundle.getByteArray(name));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static Profile getProfile(Bundle bundle, String name) {
		if (bundle == null || !bundle.containsKey(name)) return null;

		try {
			return FastPackUnpack.deserializeProfile(bundle.getByteArray(name));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public static Location getLocation(Bundle bundle, String name) {
		if (bundle == null || !bundle.containsKey(name)) return null;

		try {
			return FastPackUnpack.deserializeLocation(bundle.getByteArray(name));
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	private void BundleUtilities() {
		;
	}
}
