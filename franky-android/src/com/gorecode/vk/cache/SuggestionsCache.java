package com.gorecode.vk.cache;

import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SuggestionsCache extends UserCache {
	public static final String TABLE_NAME = "suggestions";

	@Inject
	public SuggestionsCache(SQLiteDatabase db) {
		super(db, TABLE_NAME);
	}
}
