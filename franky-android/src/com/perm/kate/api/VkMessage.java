package com.perm.kate.api;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.uva.lang.StringUtilities;

public class VkMessage {
    public String date;
    public long uid;
    public long mid;
    public String title;
    public String body;
    public boolean read_state;
    public boolean is_out;
    public ArrayList<VkAttachment> attachments=new ArrayList<VkAttachment>();
    public Long chat_id;
    public Long admin_id;
    public Integer users_count;
    public long[] chat_active;
    public Float lat;
    public Float lon;

    public ArrayList<VkMessage> fwd_messages = new ArrayList<VkMessage>();

    public static VkMessage parse(JSONObject o, boolean from_history, long history_uid, boolean from_chat, long me) throws NumberFormatException, JSONException{
        VkMessage m = new VkMessage();
        if(from_chat){
            long from_id=o.getLong("from_id");
            m.uid = from_id;
            m.is_out=(from_id==me);
        }else if(from_history){
            m.uid=history_uid;
            Long from_id = o.getLong("from_id");
            m.is_out=!(from_id==history_uid);
        }else{
            //тут не очень, потому что при получении списка диалогов если есть моё сообщение, которое я написал в беседу, то в нём uid будет мой. Хотя в других случайх uid всегда собеседника.
            m.uid = o.getLong("uid");
            m.is_out = o.optInt("out", 0)==1;
        }
        m.mid = o.optLong("mid", -1);
        m.date = o.getString("date");
        if(!from_history && !from_chat)
            m.title = Api.unescape(o.optString("title", ""));
        m.body = Api.unescape(o.getString("body"));
        m.read_state = o.optInt("read_state", 1) == 1;
        if(o.has("admin_id")) {
        	m.admin_id=o.getLong("admin_id");
        }
        if(o.has("users_count")) {
        	m.users_count=o.getInt("users_count");
        }
        if(o.has("chat_id"))
            m.chat_id=o.getLong("chat_id");
        if(o.has("chat_active")) {
        	m.chat_active = parseChatActive(o.getString("chat_active"));
        } else {
        	m.chat_active = new long[] { m.uid };
        }

        //undocumented but caught.
        //"geo":{"type":"point","coordinates":"53.195355279 45.0151262644"}
        if (o.has("geo")) {
        	JSONObject g = o.getJSONObject("geo");
        	if (g.optString("type", "").equals("point") && g.has("coordinates")) {
        		StringTokenizer tok = new StringTokenizer(g.getString("coordinates"), " ");
        		m.lat = Float.parseFloat(tok.nextToken());
        		m.lon = Float.parseFloat(tok.nextToken());
        	}
        }

        if (o.has("fwd_messages")) {
        	JSONArray f = o.getJSONArray("fwd_messages");

        	for (int i = 0; i < f.length(); i++) {
        		m.fwd_messages.add(parse(f.getJSONObject(i), false, history_uid, false, me));
        	}
        }

        JSONArray attachments=o.optJSONArray("attachments");
        if(attachments!=null)
            m.attachments=VkAttachment.parseAttachments(attachments, 0, 0);
        return m;
    }

    public static int UNREAD = 1;	 	//сообщение не прочитано 
    public static int OUTBOX = 2;	 	//исходящее сообщение 
    public static int REPLIED = 4;	 	//на сообщение был создан ответ 
    public static int IMPORTANT = 8; 	//помеченное сообщение 
    public static int CHAT = 16;    	//сообщение отправлено через диалог
    public static int FRIENDS = 32;		//сообщение отправлено другом 
    public static int SPAM = 64;		//сообщение помечено как "Спам"
    public static int DELETED = 128;	//сообщение удалено (в корзине)
    public static int FIXED = 256; 		//сообщение проверено пользователем на спам 
    public static int MEDIA = 512;		//сообщение содержит медиаконтент
    public static int GROUP_CHAT = 8192;    //беседа

    public static long[] parseChatActive(String string) {
    	long[] activeUsersIds = new long[6 * 2];

    	StringTokenizer tokenizer = new StringTokenizer(string, ",");

    	int i = 0;

    	while (tokenizer.hasMoreTokens() && i < activeUsersIds.length) {
    		activeUsersIds[i++] = Long.valueOf(tokenizer.nextToken());
    	}

    	long[] retVal = new long[i];

    	for (i = 0; i < retVal.length; i++) {
    		retVal[i] = activeUsersIds[i];
    	}

    	return retVal;
    }
}
