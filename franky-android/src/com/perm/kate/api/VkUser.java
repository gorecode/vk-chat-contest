package com.perm.kate.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

//Fields are optional. Should be null if not populated
public class VkUser {
    public long uid;
    public String first_name;
    public String last_name;
    public String nickname;
    public Integer sex=null;
    public Boolean online=null;
    public String birthdate; //bdate
    public String photo_small;
    public String photo_big;
    public Integer city=null;
    public Integer country=null;
    public Integer timezone=null;
    public String lists;
    public String domain;
    public Integer rate=null;
    public Integer university=null; //if education 
    public String university_name; //if education
    public Integer faculty=null; //if education
    public String faculty_name; //if education
    public Integer graduation=null; //if education
    public Boolean has_mobile=null;
    public String home_phone;
    public String mobile_phone;
    public String status;
    public Integer relation;
    public String friends_list_ids = null;
    public long last_seen;
    public int albums_count;
    public int videos_count;
    public int audios_count;
    public int notes_count;
    public int friends_count;
    public int user_photos_count;
    public int user_videos_count;
    //public int followers_count;
    //public int subscriptions_count;
    //public int online_friends_count;
    
    public static VkUser parse(JSONObject o) throws JSONException {
        VkUser u = new VkUser();
        u.uid = Long.parseLong(o.getString("uid"));
        if(!o.isNull("first_name"))
            u.first_name = Api.unescape(o.getString("first_name"));
        if(!o.isNull("last_name"))
            u.last_name = Api.unescape(o.getString("last_name"));
        if(!o.isNull("nickname"))
            u.nickname = Api.unescape(o.optString("nickname"));
        if(!o.isNull("domain"))
            u.domain = o.optString("domain");
        if(!o.isNull("online"))
            u.online = o.optInt("online")==1;
        if(!o.isNull("sex"))
            u.sex = Integer.parseInt(o.optString("sex"));
        if(!o.isNull("bdate"))
            u.birthdate = o.optString("bdate");
        try{
            u.city = Integer.parseInt(o.optString("city"));
        }catch(NumberFormatException ex){}
        try{            
            u.country = Integer.parseInt(o.optString("country"));
        }catch(NumberFormatException ex){}
        if(!o.isNull("timezone"))
            u.timezone = o.optInt("timezone");

        if(!o.isNull("photo_medium_rec")) {
            u.photo_small = o.optString("photo_medium_rec");
        } else if(!o.isNull("photo")) {
            u.photo_small = o.optString("photo");
        } else if(!o.isNull("photo_medium")) {
            u.photo_small = o.optString("photo_medium");
        }

        if(!o.isNull("photo_big"))
            u.photo_big = o.optString("photo_big");
        if(!o.isNull("has_mobile"))
            u.has_mobile = o.optInt("has_mobile")==1;
        if(!o.isNull("home_phone")) {
            u.home_phone = o.optString("home_phone");
            if (Strings.isNullOrEmpty(u.home_phone)) {
            	u.home_phone = null;
            }
        }
        if(!o.isNull("mobile_phone")) {
            u.mobile_phone = o.optString("mobile_phone");
            if (Strings.isNullOrEmpty(u.mobile_phone)) {
            	u.mobile_phone = null;
            }
        }
        if(!o.isNull("rate"))
            u.rate = Integer.parseInt(o.optString("rate"));
        try{
            u.faculty = Integer.parseInt(o.optString("faculty"));
        }catch(NumberFormatException ex){}
        if(!o.isNull("faculty_name"))
            u.faculty_name = o.optString("faculty_name");
        try{
            u.university = Integer.parseInt(o.optString("university"));
        }catch(NumberFormatException ex){}
        if(!o.isNull("university_name"))
            u.university_name = o.optString("university_name");
        try{
            u.graduation = Integer.parseInt(o.optString("graduation"));
        }catch(NumberFormatException ex){}
        if(!o.isNull("activity"))
            u.status = Api.unescape(o.optString("activity"));
        if(!o.isNull("relation"))
            u.relation = o.optInt("relation");
        if (!o.isNull("lists")) {
            JSONArray array = o.optJSONArray("lists");
            if (array != null) {
                String ids = "";
                for (int i=0; i<array.length()-1;++i)
                    ids += array.getString(i) + ",";
                ids += array.getString(array.length()-1);
                u.friends_list_ids = ids;
            }
        }
        if(!o.isNull("last_seen")) {
            JSONObject object = o.optJSONObject("last_seen");
            if (object != null)
                u.last_seen = object.optLong("time");
        }
        if(!o.isNull("counters")) {
            JSONObject object = o.optJSONObject("counters");
            if (object != null) {
                u.albums_count = object.optInt("albums");
                u.videos_count = object.optInt("videos");
                u.audios_count = object.optInt("audios");
                u.notes_count = object.optInt("notes");
                u.friends_count = object.optInt("friends");
                u.user_photos_count = object.optInt("user_photos");
                u.user_videos_count = object.optInt("user_videos");
                //u.online_friends_count = object.optInt("online_friends");
                //u.followers_count = object.optInt("followers");
                //u.subscriptions_count = object.optInt("subscriptions");
            }
        }
        return u;
    }
}
