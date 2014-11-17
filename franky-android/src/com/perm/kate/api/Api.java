package com.perm.kate.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.Html;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.gorecode.vk.data.Audio;
import com.gorecode.vk.data.Video;
import com.gorecode.vk.error.LongPollSessionExpiredException;
import com.uva.lang.StringUtilities;
import com.uva.location.Location;

public abstract class Api {
	public static interface CaptchaCallback {
		public String enterCaptchaKey(Captcha captcha);
	}

	public static final String HTTPS_METHODS_URL = "https://api.vk.com/method/";

	private static final int MAX_TRIES = 3;

	private final String TAG = getClass().getSimpleName();

	//TODO: it's not faster, even slower on slow devices. Maybe we should add an option to disable it. It's only good for paid internet connection.
	final static boolean COMPRESSION_ENABLED = true;

	private CaptchaCallback mOnCaptchaCallback;

	public static void throwIfError(JSONObject root) throws JSONException,KException {
		if (root.has("error") && !root.isNull("error")){
			// XXX: Special check for direct authentification.
			if (root.has("captcha_img") && root.has("captcha_sid")) {
				KException e = new KException(KException.ERROR_CAPTCHA_REQUIRED, "Captcha is needed");
				e.captcha_img = root.getString("captcha_img");
				e.captcha_sid = root.getString("captcha_sid");
				throw e;
			}

			JSONObject error = root.getJSONObject("error");

			int code = error.optInt("error_code", -1);

			String message = error.optString("error_msg", "No message");

			KException e = new KException(code, message);

			if (error.has("captcha_img") && error.has("captcha_sid")) {
				e = new KException(KException.ERROR_CAPTCHA_REQUIRED, "Captcha is needed");
				e.captcha_img = error.getString("captcha_img");
				e.captcha_sid = error.getString("captcha_sid");            	
			}

			throw e;
		}
	}

	public void setCaptchaCallback(CaptchaCallback callback) {
		mOnCaptchaCallback = callback;
	}

	abstract public String getAccessTokenForHttps();
	abstract public String getAccessToken();
	abstract public String getSecret();

	public JSONObject sendRequestViaHttp(Params params) throws IOException, MalformedURLException, JSONException, KException {
		return sendRequest(params, GET_HTTP_URL);
	}

	public JSONObject sendRequestViaHttps(Params params) throws IOException, MalformedURLException, JSONException, KException {
		return sendRequest(params, GET_HTTPS_URL);
	}

	private final Function<Params, String> GET_HTTP_URL = new Function<Params, String>() {
		@Override
		public String apply(Params params) {
			return getUrlForHttpRequest(params);
		}
	};

	private final Function<Params, String> GET_HTTPS_URL = new Function<Params, String>() {
		@Override
		public String apply(Params params) {
			return getUrlForHttpsRequest(params);
		}
	};

	public JSONObject sendRequest(Params params, Function<Params, String> toRequestUrlFunc) throws IOException, MalformedURLException, JSONException, KException {
		String url = toRequestUrlFunc.apply(params);

		try {
			return sendRequest(url);
		} catch (KException e) {
			if (e.error_code == KException.ERROR_CAPTCHA_REQUIRED) {
				Captcha captcha = new Captcha();

				captcha.img = e.captcha_img;
				captcha.sid = e.captcha_sid;

				String captchaKey = enterCaptcha(captcha);

				if (captchaKey != null) {
					params.put("captcha_sid", captcha.sid);
					params.put("captcha_key", captchaKey);

					return sendRequest(params, toRequestUrlFunc);
				}
			}

			throw e;
		}
	}

	private String enterCaptcha(Captcha captcha) {
		if (mOnCaptchaCallback == null) return null;

		return mOnCaptchaCallback.enterCaptchaKey(captcha);
	}

	private JSONObject sendRequest(String url) throws IOException, MalformedURLException, JSONException, KException {
		Log.i(TAG, "url="+url);
		String response="";
		for(int i=1;i<=MAX_TRIES;++i){
			try{
				if(i!=1)
					Log.i(TAG, "try "+i);
				response = sendRequestInternal(url);
				break;
			}catch(javax.net.ssl.SSLException ex){
				processNetworkException(i, ex);
			}catch(java.net.SocketException ex){
				processNetworkException(i, ex);
			}
		}
		Log.i(TAG, "response="+response);
		JSONObject root=new JSONObject(response);

		throwIfError(root);

		return root;
	}

	private void processNetworkException(int i, IOException ex) throws IOException {
		ex.printStackTrace();
		if(i==MAX_TRIES)
			throw ex;
	}

	private String sendRequestInternal(String url) throws IOException, MalformedURLException, WrongResponseCodeException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpParams httpParams = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
		HttpConnectionParams.setSoTimeout(httpParams, 30000);

		HttpUriRequest httpRequest = new HttpGet(url);

		if (COMPRESSION_ENABLED) {
			httpRequest.addHeader("Accept-Encoding", "gzip");
		}

		HttpResponse httpResponse = httpClient.execute(httpRequest);

		InputStream inStream = httpResponse.getEntity().getContent();

		Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");

		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			inStream = new GZIPInputStream(inStream);
		}

		return convertStreamToString(inStream);
	}

	private String getUrlForHttpRequest(Params params) {
		try {
			String paramsString = params.getParamsString();
			if (paramsString.length() != 0) {
				paramsString += "&";
			}
			paramsString += "access_token=" + getAccessToken();
			String method = "/method/" + params.method_name + "?" + paramsString;
			String sigInput = URLDecoder.decode(method + getSecret(), "UTF-8");
			String sig = getMd5HashFromString(sigInput);
			String url = "http://api.vk.com" + method + "&sig=" + sig;
			return url;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getUrlForHttpsRequest(Params params) {
		String args = params.getParamsString();

		//add access_token
		if(args.length()!=0)
			args+="&";
		if (getAccessToken() != null) {
			args+="access_token="+getAccessTokenForHttps();
		}

		return HTTPS_METHODS_URL+params.method_name+"?"+args;
	}

	public static String unescape(String text){
		return Html.fromHtml(text).toString();
	}

	<T> String arrayToString(Collection<T> items) {
		if(items==null)
			return null;
		String str_cids = "";
		for (Object item:items){
			if(str_cids.length()!=0)
				str_cids+=',';
			str_cids+=item;
		}
		return str_cids;
	}

	//*** methods for users ***//
	//http://vkontakte.ru/developers.php?o=-1&p=getProfiles
	public ArrayList<VkUser> getProfiles(Collection<Long> uids, ArrayList<String> domains, String fields, String name_case) throws MalformedURLException, IOException, JSONException, KException{
		if (uids == null && domains == null)
			return null;
		if ((uids != null && uids.size() == 0) || (domains != null && domains.size() == 0))
			return null;
		Params params = new Params("getProfiles");
		if (uids != null && uids.size() > 0)
			params.put("uids",arrayToString(uids));
		if (domains != null && domains.size() > 0)
			params.put("domains",arrayToString(domains));
		if (fields == null)
			params.put("fields","uid,first_name,last_name,nickname,domain,sex,bdate,city,country,timezone,photo,photo_medium,photo_medium_rec,photo_big,has_mobile,rate,contacts,education,online");
		else
			params.put("fields",fields);
		if (name_case == null) {
			name_case = "nom"; 
		}
		params.put("name_case",name_case);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array=root.optJSONArray("response");
		return parseUsers(array);
	}

	public static ArrayList<VkUser> parseUsers(JSONArray array) throws JSONException {
		ArrayList<VkUser> users=new ArrayList<VkUser>();
		//it may be null if no users returned
		//no users may be returned if we request users that are already removed
		if(array==null)
			return users;
		int category_count=array.length();
		for(int i=0; i<category_count; ++i){
			if(array.get(i)==null || ((array.get(i) instanceof JSONObject)==false))
				continue;
			JSONObject o = (JSONObject)array.get(i);
			VkUser u = VkUser.parse(o);
			users.add(u);
		}
		return users;
	}

	/*** methods for friends ***/
	//http://vkontakte.ru/developers.php?o=-1&p=friends.get
	public ArrayList<VkUser> getFriends(Long user_id, String fields, Integer lid) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("friends.get");
		if(fields==null)
			fields="first_name,last_name,photo_medium,online";
		params.put("fields",fields);
		params.put("uid",user_id);
		params.put("lid", lid);

		//сортировка по популярности не даёт запросить друзей из списка
		if(lid==null)
			params.put("order","hints");

		JSONObject root = sendRequestViaHttp(params);
		ArrayList<VkUser> users=new ArrayList<VkUser>();
		JSONArray array=root.optJSONArray("response");
		//if there are no friends "response" will not be array
		if(array==null)
			return users;
		int category_count=array.length();
		for(int i=0; i<category_count; ++i){
			JSONObject o = (JSONObject)array.get(i);
			VkUser u = VkUser.parse(o);
			users.add(u);
		}
		return users;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=friends.getOnline
	public ArrayList<Long> getOnlineFriends(Long uid) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("friends.getOnline");
		params.put("uid",uid);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array=root.optJSONArray("response");
		ArrayList<Long> users=new ArrayList<Long>();
		if (array != null) {
			int category_count=array.length();
			for(int i=0; i<category_count; ++i){
				Long id = array.optLong(i, -1);
				if(id!=-1)
					users.add(id);
			}
		}
		return users;
	}

	private void addCaptchaParams(String captcha_key, String captcha_sid, Params params) {
		params.put("captcha_sid",captcha_sid);
		params.put("captcha_key",captcha_key);
	}

	/*** methods for messages 
	 * @throws KException ***/
	//http://vkontakte.ru/developers.php?o=-1&p=messages.get
	public ArrayList<VkMessage> getMessages(long time_offset, boolean is_out, int count) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("messages.get");
		if (is_out)
			params.put("out","1");
		if (time_offset!=0)
			params.put("time_offset", time_offset);
		if (count != 0)
			params.put("count", count);
		params.put("preview_length","0");
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array = root.optJSONArray("response");
		ArrayList<VkMessage> messages = parseMessages(array, false, 0, false, 0);
		return messages;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=messages.getHistory
	public ArrayList<VkMessage> getMessagesHistory(long uid, long chat_id, long me, Long offset, int count) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("messages.getHistory");
		if(chat_id<=0)
			params.put("uid",uid);
		else
			params.put("chat_id",chat_id);
		params.put("offset", offset);
		if (count != 0)
			params.put("count", count);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array = root.optJSONArray("response");
		ArrayList<VkMessage> messages = parseMessages(array, chat_id<=0, uid, chat_id>0, me);
		return messages;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=messages.getDialogs
	public ArrayList<VkMessage> getMessagesDialogs(long offset, int count) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("messages.getDialogs");
		params.put("offset", offset);
		params.put("count", count);
		params.put("preview_length","0");
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array = root.optJSONArray("response");
		ArrayList<VkMessage> messages = parseMessages(array, false, 0, false ,0);
		return messages;
	}

	public static ArrayList<VkMessage> parseMessages(JSONArray array, boolean from_history, long history_uid, boolean from_chat, long me) throws JSONException {
		ArrayList<VkMessage> messages = new ArrayList<VkMessage>();
		if (array != null) {
			int category_count = array.length();
			for(int i = 1; i < category_count; ++i) {
				JSONObject o = (JSONObject)array.get(i);
				VkMessage m = VkMessage.parse(o, from_history, history_uid, from_chat, me);
				messages.add(m);
			}
		}
		return messages;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=messages.send
	public String sendMessage(Long uid, Long chat_id, String message, String title, String type, ArrayList<String> attachments, Location location, Collection<Long> forwardedMessages, String captcha_key, String captcha_sid) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("messages.send");
		if(uid != null)
			params.put("uid", uid);
		if(chat_id != null)
			params.put("chat_id", chat_id);
		params.put("message", message);
		params.put("title", title);
		params.put("type", type);
		if (location != null) {
			params.put("lat", (float)location.latitude);
			params.put("long", (float)location.longitude);
		}
		if(attachments !=null && attachments.size() > 0) {
			String attachments_params = attachments.get(0);
			for (int i=1;i<attachments.size();i++)
				attachments_params = attachments_params + "," + attachments.get(i);
			params.put("attachment", attachments_params);
		}
		if (forwardedMessages != null) {
			params.put("forward_messages", getUidsString(forwardedMessages));
		}
		addCaptchaParams(captcha_key, captcha_sid, params);
		JSONObject root = sendRequestViaHttp(params);
		Object message_id = root.opt("response");
		if (message_id != null)
			return String.valueOf(message_id);
		return null;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=messages.markAsNew
	//http://vkontakte.ru/developers.php?o=-1&p=messages.markAsRead
	public String markAsNewOrAsRead(Collection<Long> mids, boolean as_read) throws MalformedURLException, IOException, JSONException, KException{
		if (mids == null || mids.size() == 0)
			return null;
		Params params;
		if (as_read)
			params = new Params("messages.markAsRead");
		else
			params = new Params("messages.markAsNew");
		params.put("mids", arrayToString(mids));
		JSONObject root = sendRequestViaHttp(params);
		Object response_code = root.opt("response");
		if (response_code != null)
			return String.valueOf(response_code);
		return null;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=messages.delete
	public String deleteMessage(Long mid) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("messages.delete");
		params.put("mid", mid);
		JSONObject root = sendRequestViaHttp(params);
		Object response_code = root.opt("response");
		if (response_code != null)
			return String.valueOf(response_code);
		return null;
	}

	/*** for audio ***/
	//http://vkontakte.ru/developers.php?o=-1&p=audio.get
	public ArrayList<Audio> getAudio(Long uid, Long gid, ArrayList<Long> aids) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("audio.get");
		params.put("uid", uid);
		params.put("gid", gid);
		params.put("aids", arrayToString(aids));
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array = root.optJSONArray("response");
		return parseAudioList(array, 0);
	}

	/*** for video ***/
	//http://vkontakte.ru/developers.php?o=-1&p=video.get //width = 130,160,320
	public ArrayList<Video> getVideo(String videos, Long owner_id, String width, Long count, Long offset) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("video.get");
		params.put("videos", videos);
		if (owner_id != null){
			if(owner_id>0)
				params.put("uid", owner_id);
			else
				params.put("gid", -owner_id);
		}
		params.put("width", width);
		params.put("count", count);
		params.put("offset", offset);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array = root.optJSONArray("response");
		ArrayList<Video> videoss = new ArrayList<Video>();
		if (array != null) {
			for(int i = 1; i<array.length(); ++i) {
				JSONObject o = (JSONObject)array.get(i);
				Video video = Video.fromJson(o);
				videoss.add(video);
			}
		}
		return videoss;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=photos.getUploadServer
	public String photosGetUploadServer(long album_id, Long group_id) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("photos.getUploadServer");
		params.put("aid",album_id);
		params.put("gid",group_id);
		JSONObject root = sendRequestViaHttp(params);
		JSONObject response = root.getJSONObject("response");
		return response.getString("upload_url");
	}

	//http://vkontakte.ru/developers.php?o=-1&p=photos.getWallUploadServer
	public String photosGetWallUploadServer(Long user_id, Long group_id) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("photos.getWallUploadServer");
		params.put("uid",user_id);
		params.put("gid",group_id);
		JSONObject root = sendRequestViaHttp(params);
		JSONObject response = root.getJSONObject("response");
		return response.getString("upload_url");
	}

	//http://vkontakte.ru/developers.php?oid=-1&p=getAudioUploadServer
	public String getAudioUploadServer() throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("getAudioUploadServer");
		JSONObject root = sendRequestViaHttp(params);
		JSONObject response = root.getJSONObject("response");
		return response.getString("upload_url");
	}

	//http://vkontakte.ru/developers.php?oid=-1&p=photos.getMessagesUploadServer
	public String getMessagesUploadServer() throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("photos.getMessagesUploadServer");
		JSONObject root = sendRequestViaHttp(params);
		JSONObject response = root.getJSONObject("response");
		return response.getString("upload_url");
	}

	//http://vkontakte.ru/developers.php?o=-1&p=photos.getProfileUploadServer
	public String photosGetProfileUploadServer() throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("photos.getProfileUploadServer");
		JSONObject root = sendRequestViaHttp(params);
		JSONObject response = root.getJSONObject("response");
		return response.getString("upload_url");
	}

	//http://vkontakte.ru/developers.php?o=-1&p=photos.save
	public ArrayList<VkPhoto> photosSave(String server, String photos_list, Long aid, Long group_id, String hash) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("photos.save");
		params.put("server",server);
		params.put("photos_list",photos_list);
		params.put("aid",aid);
		params.put("gid",group_id);
		params.put("hash",hash);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array=root.getJSONArray("response");
		ArrayList<VkPhoto> photos = parsePhotos(array);
		return photos;
	}

	//http://vkontakte.ru/developers.php?oid=-1&p=photos.saveMessagesPhoto
	public ArrayList<VkPhoto> saveMessagesPhoto(String server, String photo, String hash) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("photos.saveMessagesPhoto");
		params.put("server",server);
		params.put("photo",photo);
		params.put("hash",hash);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array=root.getJSONArray("response");
		ArrayList<VkPhoto> photos = parsePhotos(array);
		return photos;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=photos.saveProfilePhoto
	public String[] saveProfilePhoto(String server, String photo, String hash) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("photos.saveProfilePhoto");
		params.put("server",server);
		params.put("photo",photo);
		params.put("hash",hash);
		JSONObject root = sendRequestViaHttp(params);
		JSONObject response = root.getJSONObject("response");
		String src = response.optString("photo_src");
		String hash1 = response.optString("photo_hash");
		String[] res=new String[]{src, hash1};
		return res;
	}

	private ArrayList<VkPhoto> parsePhotos(JSONArray array) throws JSONException {
		ArrayList<VkPhoto> photos=new ArrayList<VkPhoto>(); 
		int category_count=array.length(); 
		for(int i=0; i<category_count; ++i){
			//in getUserPhotos first element is integer
			if(array.get(i) instanceof JSONObject == false)
				continue;
			JSONObject o = (JSONObject)array.get(i);
			VkPhoto p = VkPhoto.parse(o);
			photos.add(p);
		}
		return photos;
	}

	//http://vkontakte.ru/developers.php?o=-1&p=photos.getById
	public ArrayList<VkPhoto> getPhotosById(String photos) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("photos.getById");
		params.put("photos", photos);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array = root.optJSONArray("response");
		if (array == null)
			return new ArrayList<VkPhoto>(); 
		ArrayList<VkPhoto> photos1 = parsePhotos(array);
		return photos1;
	}

	//no documentation
	public ArrayList<VkUser> searchUser(String q, String fields, Long count, Long offset) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("users.search");
		params.put("q", q);
		params.put("count", count);
		params.put("offset", offset);
		params.put("fields", fields);
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array=root.optJSONArray("response");
		return parseUsers(array);
	}

	//http://vk.com/pages?oid=-1&p=%D0%9F%D0%BE%D0%B4%D0%BA%D0%BB%D1%8E%D1%87%D0%B5%D0%BD%D0%B8%D0%B5_%D0%BA_LongPoll_%D1%81%D0%B5%D1%80%D0%B2%D0%B5%D1%80%D1%83
	public LongPollServiceResponse getLongPollServerUpdates(LongPollServerInfo info) throws Exception {
		JSONObject response = sendRequest(info.toUrl());

		if (response.has("failed")) {
			throw new LongPollSessionExpiredException();
		}

		return LongPollServiceResponse.parse(response); 
	}

	//http://vkontakte.ru/developers.php?o=-1&p=messages.getLongPollServer
	public LongPollServerInfo getLongPollServer() throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("messages.getLongPollServer");
		JSONObject root = sendRequestViaHttp(params);
		JSONObject response = root.getJSONObject("response");
		LongPollServerInfo info = new LongPollServerInfo();
		info.key=response.getString("key");
		info.server=response.getString("server");
		info.ts=response.getLong("ts");
		return info;
	}

	//не документирован
	public void setOnline() throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("activity.online");
		sendRequestViaHttp(params);
	}

	//http://vkontakte.ru/developers.php?oid=-1&p=friends.add
	public long addFriend(Long uid, String text, String captcha_key, String captcha_sid) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("friends.add");
		params.put("uid", uid);
		params.put("text", text);
		addCaptchaParams(captcha_key, captcha_sid, params);
		JSONObject root = sendRequestViaHttp(params);
		return root.optLong("response");
	}

	//http://vkontakte.ru/developers.php?oid=-1&p=friends.delete
	public long deleteFriend(Long uid) throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("friends.delete");
		params.put("uid", uid);
		JSONObject root = sendRequestViaHttp(params);
		return root.optLong("response");
	}

	//http://vkontakte.ru/developers.php?oid=-1&p=friends.getRequests
	public ArrayList<Object[]> getRequestsFriends() throws MalformedURLException, IOException, JSONException, KException{
		Params params = new Params("friends.getRequests");
		params.put("need_messages", "1");
		JSONObject root = sendRequestViaHttp(params);
		JSONArray array=root.optJSONArray("response");
		ArrayList<Object[]> users=new ArrayList<Object[]>();
		if (array != null) {
			int category_count=array.length();
			for(int i=0; i<category_count; ++i) {
				JSONObject item = array.optJSONObject(i);
				if (item != null) {
					Long id = item.optLong("uid", -1);
					if (id!=-1) {
						Object[] u = new Object[2];
						u[0] = id;
						u[1] = item.optString("message");
						users.add(u);
					}
				}
			}
		}
		return users;
	}

	//http://vkontakte.ru/pages?oid=-1&p=messages.deleteDialog
	public int deleteMessageThread(Long uid, Long chatId) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("messages.deleteDialog");
		params.put("uid", uid);
		params.put("chat_id", chatId);
		JSONObject root = sendRequestViaHttp(params);
		return root.getInt("response");
	}

	//http://vkontakte.ru/developers.php?oid=-1&p=execute
	public JSONObject execute(String code) throws MalformedURLException, IOException, JSONException, KException {
		Params params = new Params("execute");
		params.put("code", code);
		return sendRequestViaHttp(params).getJSONObject("response");
	}

	public static String extractPattern(String string, String pattern){
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(string);
		if (!m.find())
			return null;
		return m.toMatchResult().group(1);
	}

	public static String convertStreamToString(InputStream is) throws IOException {
		InputStreamReader r = new InputStreamReader(is);
		StringWriter sw = new StringWriter();
		char[] buffer = new char[1024];
		try {
			for (int n; (n = r.read(buffer)) != -1;) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedIOException();
				}
				sw.write(buffer, 0, n);
			}
		}
		finally{
			try {
				is.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return sw.toString();
	}

	protected static String getUidsString(Collection<Long> uids) {
		return StringUtilities.join(",", Iterables.toArray(Iterables.transform(uids, Functions.toStringFunction()), String.class));
	}

	public static final String getMd5HashFromString(String value) {
		final MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(value.getBytes("UTF-8"));

			byte[] digest = md5.digest();
			BigInteger bigInt = new BigInteger(1, digest);

			String hashString = bigInt.toString(16);

			while(hashString.length() < 32 ){
				hashString = "0" + hashString;
			}

			return hashString;
		} catch (Exception ex) {
			return null;
		}
	} 

	/** 
	 * Convert byte array to hex string. 
	 * 
	 * @param data 
	 *            Target data array. 
	 * @return Hex string. 
	 */ 
	private static final String convertToHex(byte[] data) { 
		if (data == null || data.length == 0) { 
			return null; 
		} 

		final StringBuffer buffer = new StringBuffer(); 
		for (int byteIndex = 0; byteIndex < data.length; byteIndex++) { 
			int halfbyte = (data[byteIndex] >>> 4) & 0x0F; 
			int two_halfs = 0; 
			do { 
				if ((0 <= halfbyte) && (halfbyte <= 9)) 
					buffer.append((char) ('0' + halfbyte)); 
				else 
					buffer.append((char) ('a' + (halfbyte - 10))); 
				halfbyte = data[byteIndex] & 0x0F; 
			} while (two_halfs++ < 1); 
		} 

		return buffer.toString(); 
	}

	private ArrayList<Audio> parseAudioList(JSONArray array, int type_array) //type_array must be 0 or 1
			throws JSONException {
		ArrayList<Audio> audios = new ArrayList<Audio>();
		if (array != null) {
			for(int i = type_array; i<array.length(); ++i) { //get(0) is integer, it is audio count
				JSONObject o = (JSONObject)array.get(i);
				audios.add(Audio.parse(o));
			}
		}
		return audios;
	}
}


