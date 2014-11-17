package com.gorecode.vk.data;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Document implements Serializable {
	private static final long serialVersionUID = 4377001733555094206L;

	public long documentId;
	public long ownerId;
	public int size;
	public String title;
	public String url;
	public String extension;

	public static Document fromJson(JSONObject json) throws JSONException {
		Document document = new Document();

		document.documentId = json.getLong("did");
		document.ownerId = json.getLong("owner_id");
		document.size = json.getInt("size");
		document.title = json.getString("title");
		document.extension = json.getString("ext");
		document.url = json.getString("url");

		return document;
	}
}
