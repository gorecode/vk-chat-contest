package com.perm.kate.api;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class UploadedObject implements Serializable {
	private static final long serialVersionUID = 102398747209706803L;

	public String server;
	public String photo;
	public String hash;

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();

		try {
			json.put("server", server);
			json.put("photo", photo);
			json.put("hash", hash);
		} catch (JSONException e) {
			// Should never happen.
		}

		return json;
	}

	public static UploadedObject parseJson(JSONObject json) throws JSONException {
		UploadedObject obj = new UploadedObject();
		obj.server = json.getString("server");
		obj.photo = json.getString("photo");
		obj.hash = json.getString("hash");
		return obj;
	}
}
