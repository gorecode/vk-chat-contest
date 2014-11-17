package com.gorecode.vk.cache;

import java.util.Collection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.uva.log.Log;

public class SQLiteOpenHelperEx extends SQLiteOpenHelper {
	public static class TableDescription {
		public final String name;
		public final String creationSqlCommand;

		public static TableDescription forBlobTable(String tableName) {
			return new TableDescription(tableName, SQLiteBlobObjectCache.getCreateTableCommandForName(tableName));
		}

		private TableDescription(String name, String creationCommand) {
			this.name = name;
			this.creationSqlCommand = creationCommand;
		}
	}

	private static final String TAG = SQLiteOpenHelperEx.class.getSimpleName();

	private final Collection<TableDescription> mTables;

	public SQLiteOpenHelperEx(Context context, String dbFilename, int version, Collection<TableDescription> tables) {
		super(context, dbFilename, null, version);

		mTables = tables;
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
		for (TableDescription table : mTables) { 
			db.execSQL(table.creationSqlCommand);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (TableDescription table : mTables) { 
			dropTableIfExists(db, table.name);
		}
		onCreate(db);		
	}

	private void dropTableIfExists(SQLiteDatabase db, String tableName) {
		try {
			db.execSQL("drop table if exists " +  tableName + ";\n");
		} catch (SQLiteException e) {
			Log.exception(TAG, e);
		}
	}
}
