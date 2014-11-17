package com.gorecode.vk.data;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.perm.kate.api.Api;

public class Audio implements Serializable {
	private static final long serialVersionUID = -6708936245676741301L;

	public long aid;
    public long ownerId;
    public String artist;
    public String title;
    public long duration;
    public String url;
    public Long lyricsId;

	public static boolean isSame(Audio o1, Audio o2) {
		if (o1 == null || o2 == null) return false;

		return o1.aid == o2.aid;
	}

    public static Audio parse(JSONObject o) throws NumberFormatException, JSONException{
        Audio audio = new Audio();
        audio.aid = Long.parseLong(o.getString("aid"));
        audio.ownerId = Long.parseLong(o.getString("owner_id"));
        if(o.has("performer"))
            audio.artist = Api.unescape(o.getString("performer"));
        else if(o.has("artist"))
            audio.artist = Api.unescape(o.getString("artist"));
        audio.title = Api.unescape(o.getString("title"));
        audio.duration = Long.parseLong(o.getString("duration"));
        audio.url = o.optString("url", null);
        
        String tmp=o.optString("lyrics_id");
        if(tmp!=null && !tmp.equals(""))//otherwise lyrics_id=null 
            audio.lyricsId = Long.parseLong(tmp);
        return audio;
    }
}