package com.gorecode.vk.data;

import java.io.Serializable;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.perm.kate.api.Api;

public class Video implements Serializable {
	private static final long serialVersionUID = 2361813822623455212L;

	public long vid;
    public long ownerId;
    public String title;
    public String description;
    public long duration;
    public String link;
    public String image;
    public long date;
    public String player;

    public String url240;
    public String url320;
    public String url480;
    public String url720;
    public String urlExternal;

    public static Video fromJson(JSONObject o) throws NumberFormatException, JSONException{
        Video v = new Video();
        if(o.has("vid"))
            v.vid = o.getLong("vid");
        if(o.has("id"))//video.getUserVideos
            v.vid = Long.parseLong(o.getString("id"));
        v.ownerId = o.getLong("owner_id");
        v.title = Api.unescape(o.getString("title"));
        v.duration = o.getLong("duration");
        v.description = Api.unescape(o.optString("description"));
        if(o.has("image"))
            v.image = o.optString("image");
        if(o.has("thumb"))//video.getUserVideos
            v.image = o.optString("thumb");
        v.link = o.optString("link");
        v.date = o.optLong("date");
        v.player = o.optString("player");

        if (o.has("files")) {
        	JSONObject f = o.getJSONObject("files");

        	Iterator<String> keys = f.keys();

        	while (keys.hasNext()) {
        		String key = keys.next();
        		String value = f.getString(key);

        		if (key.equals("external")) {
        			v.urlExternal = value;
        		}
        		if (key.equals("mp4_240")) {
        			v.url240 = value;
        		}
        		if (key.equals("flv_320") || key.equals("mp4_320")) {
        			v.url320 = value;
        		}
        		if (key.equals("mp4_480")) {
        			v.url480 = value;
        		}
        		if (key.equals("mp4_720")) {
        			v.url720 = value;
        		}
        	}
        }

        return v;
    }

    public boolean hasUrls() {
    	return urlExternal != null || url240 != null || url320 != null || url480 != null || url720 != null;
    }
}
