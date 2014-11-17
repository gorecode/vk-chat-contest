package com.gorecode.vk.cache;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import android.database.sqlite.SQLiteDatabase;

@Singleton
public class OffersCache extends UserCache {
	public static final String TABLE_NAME = "offers";

	@Inject
	public OffersCache(SQLiteDatabase db) {
		super(db, TABLE_NAME);
	}
}
