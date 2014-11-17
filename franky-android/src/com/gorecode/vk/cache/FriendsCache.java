package com.gorecode.vk.cache;

import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendsCache extends UserCache {
	public static final String TABLE_NAME = "friends";

	@Inject
	public FriendsCache(SQLiteDatabase db) {
		super(db, TABLE_NAME);

		setTimelimitForDeserialization(Long.MAX_VALUE);
	}
}
