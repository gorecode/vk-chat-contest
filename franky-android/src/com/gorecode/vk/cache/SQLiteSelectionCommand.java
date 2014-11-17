package com.gorecode.vk.cache;

import java.io.IOException;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.uva.lang.StringUtilities;
import com.uva.log.Log;
import com.uva.utilities.AssertCompat;

/**
 * Wraps ugly SQLite and ContentProvider query interfaces.
 *
 * @author vvs.
 */
public class SQLiteSelectionCommand {
	private static final String TAG = "SelectionQuery";

	private Object mFrom;
	private String[] mColumns;
	private String mWhere;
	private String[] mArgs;
	private String mOrderBy;
	private boolean mDistinct;

	public SQLiteSelectionCommand select(String... columns) {
		mColumns = columns;
		return this;
	}

	public SQLiteSelectionCommand distinct(boolean distinct) {
		mDistinct = distinct;
		return this;
	}

	public SQLiteSelectionCommand from(String table) {
		mFrom = table;
		return this;
	}

	public SQLiteSelectionCommand from(Uri uri) {
		mFrom = uri;
		return this;
	}

	public SQLiteSelectionCommand where(String condition) {
		mWhere = condition;
		return this;
	}

	public SQLiteSelectionCommand orderByAscending(String column) {
		if (StringUtilities.isEmpty(column)) throw new IllegalArgumentException();
		mOrderBy = column + " asc";
		return this;
	}

	public SQLiteSelectionCommand orderByDescending(String column) {
		if (StringUtilities.isEmpty(column)) throw new IllegalArgumentException();
		mOrderBy = column + " desc";
		return this;
	}

	public SQLiteSelectionCommand withValues(Object... values) {
		int length = values.length;
		if (mArgs == null || mArgs.length != length) {
			mArgs = new String[length];
		}			

		for (int i = 0; i < length; i++) {
			mArgs[i] = (values[i] != null) ? values[i].toString() : null; 
		}		
		return this;
	}

	public Cursor execute(SQLiteDatabase db) throws Exception {
		AssertCompat.isTrue(mFrom instanceof String, "Illegal table name");
		try {
			Cursor c = db.query(mDistinct, (String)mFrom, mColumns, mWhere, mArgs, null, null, mOrderBy, null);
			if (c == null) throw new IOException("Can't query from " + mFrom);
			return c;
		}
		catch (RuntimeException e) {
			throw new Exception(e);
		}
	}

	public Cursor execute(ContentResolver resolver) throws Exception {
		AssertCompat.isTrue(mFrom instanceof Uri, "Illegal table uri");
		try {
			if (mDistinct) {
				Log.warning(TAG, "Distinct selection is not supported by ContentProvider");
			}
			Cursor c = resolver.query((Uri)mFrom, mColumns, mWhere, mArgs, mOrderBy);
			if (c == null) throw new Exception("Can't query from " + mFrom);
			return c;
		} catch (RuntimeException e) {
			throw new Exception(e);
		}
	}
}	
