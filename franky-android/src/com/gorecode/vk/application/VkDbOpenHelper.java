package com.gorecode.vk.application;

import java.util.ArrayList;
import java.util.Collection;

import android.content.Context;

import com.gorecode.vk.cache.ChatCache;
import com.gorecode.vk.cache.DialogsCache;
import com.gorecode.vk.cache.FriendsCache;
import com.gorecode.vk.cache.OffersCache;
import com.gorecode.vk.cache.SQLiteOpenHelperEx;
import com.gorecode.vk.cache.SuggestionsCache;

public class VkDbOpenHelper extends SQLiteOpenHelperEx {
	public static final int DB_VERSION = 19;

	private static final String DB_FILENAME = "data_cache.db";

	private static Collection<TableDescription> getTables() {
		ArrayList<TableDescription> tables = new ArrayList<SQLiteOpenHelperEx.TableDescription>();
		tables.add(TableDescription.forBlobTable(ChatCache.TABLE_NAME));
		tables.add(TableDescription.forBlobTable(DialogsCache.TABLE_NAME));
		tables.add(TableDescription.forBlobTable(FriendsCache.TABLE_NAME));
		tables.add(TableDescription.forBlobTable(OffersCache.TABLE_NAME));
		tables.add(TableDescription.forBlobTable(SuggestionsCache.TABLE_NAME));
		return tables;
	}

	public VkDbOpenHelper(Context context) {
		super(context, DB_FILENAME, DB_VERSION, getTables());
	}
}
