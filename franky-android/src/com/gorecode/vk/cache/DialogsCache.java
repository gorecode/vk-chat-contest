package com.gorecode.vk.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import com.gorecode.vk.activity.dialogs.DialogsFragment;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.FastPackUnpack;

public class DialogsCache extends SQLiteBlobObjectCache<Dialog> {
	public static final String TABLE_NAME = "dialogs";

	private static final String PREFERENCE_NAME = DialogsFragment.class.getName();
	private static final String PREFERENCE_LAST_UPDATE_TIME = "dialogsFragment:lastUpdateTime";

	private final SharedPreferences mPreferences;

	public DialogsCache(SQLiteDatabase db) {
		super(db, TABLE_NAME);

		mPreferences = getSharedPreferences();
	}

	public long getLastUpdateTime() {
		return mPreferences.getLong(PREFERENCE_LAST_UPDATE_TIME, 0);
	}

	public void setLastUpdateTime(long timeMillis) {
		mPreferences.edit().putLong(PREFERENCE_LAST_UPDATE_TIME, timeMillis).commit();
	}

	public void clearLastUpdateTime() {
		mPreferences.edit().remove(PREFERENCE_LAST_UPDATE_TIME).commit();
	}

	public Dialog findByCid(long cid) {
		return findOne(getKeyForCid(cid));
	}

	@Override
	public String getEntityId(Dialog entity) {
		return getKeyForCid(entity.getCid());
	}

	@Override
	public long getEntityActualityTime(Dialog entity) {
		return entity.lastMessage.timestamp;
	}

	public static String getKeyForCid(long cid) {
		return String.format("cid:%d", cid);
	}

	protected byte[] serializeEntity(Dialog entity) throws Exception {
		ByteArrayOutputStream memStream = new ByteArrayOutputStream();
		FastPackUnpack.writeDialog(new DataOutputStream(memStream), entity);
		return memStream.toByteArray();
	}

	protected Dialog deserializeEntity(byte[] blob) throws Exception {
		return FastPackUnpack.readDialog(new DataInputStream(new ByteArrayInputStream(blob)));
	}

	private static SharedPreferences getSharedPreferences() {
		return VkApplication.getApplication().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
	}
}
