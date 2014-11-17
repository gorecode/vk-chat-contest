package com.gorecode.vk.cache;

import android.database.sqlite.SQLiteDatabase;

import com.gorecode.vk.data.FastPackUnpack;
import com.gorecode.vk.data.Profile;

public class UserCache extends SQLiteBlobObjectCache<Profile> {
	public UserCache(SQLiteDatabase db, String tableName) {
		super(db, tableName);
	}

	@Override
	protected String getEntityId(Profile entity) {
		return String.valueOf(entity.id);
	}

	@Override
	protected long getEntityActualityTime(Profile entity) {
		return System.currentTimeMillis();
	}

	@Override
	protected byte[] serializeEntity(Profile entity) throws Exception {
		return FastPackUnpack.serializeProfile(entity);
	}

	@Override
	protected Profile deserializeEntity(byte[] blob) throws Exception {
		return FastPackUnpack.deserializeProfile(blob);
	}
}
