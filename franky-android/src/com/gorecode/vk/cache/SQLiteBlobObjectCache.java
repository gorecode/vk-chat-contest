package com.gorecode.vk.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Process;

import com.google.common.base.Preconditions;
import com.uva.lang.ThreadFactories;
import com.uva.log.Log;

/**
 * @author enikey, vvs.
 */
public abstract class SQLiteBlobObjectCache<T /*extends Serializable*/> {
	private static long DEFAULT_TIMELIMIT_FOR_DESERIALIZATION = 250;

	private static int DEFAULT_MANDATORY_OBJECTS_COUNT_FOR_DESERIALIZATION = 25;

	private static ExecutorService sBackgroundExecutor = Executors.newFixedThreadPool(1, ThreadFactories.WITH_LOWEST_PRIORITY);

	final String TAG = getClass().getSimpleName();

	public interface Columns {
		String _ID = "_id";
		String ACTUALITY_TIME = "actuality_time";
		String DATA = "data";
	}

	final SQLiteDatabase mDb;

	final String mTableName;

	private final SQLiteStatement mReplace;
	private final SQLiteStatement mDeleteAll;
	private final SQLiteStatement mDeleteById;
	private final SQLiteStatement mDeleteWherePkLike;

	private long mTimelimitForDeserialization = DEFAULT_TIMELIMIT_FOR_DESERIALIZATION;

	private int mMandatoryObjectsCountForDeserialization = DEFAULT_MANDATORY_OBJECTS_COUNT_FOR_DESERIALIZATION;

	public SQLiteBlobObjectCache(SQLiteDatabase db, String tableName) {
		mTableName = tableName;

		mDb = db;

		mReplace = mDb.compileStatement(getReplaceCommand());
		mDeleteAll = mDb.compileStatement(getDeleteAllCommand());
		mDeleteById = mDb.compileStatement(getDeleteByIdCommand());
		mDeleteWherePkLike = db.compileStatement(String.format("delete from %s where %s like ?;", tableName, Columns._ID));
	}

	public void setTimelimitForDeserialization(long timelimit) {
		Preconditions.checkArgument(timelimit > 0);

		mTimelimitForDeserialization = timelimit;
	}

	public void createIfNotExists() throws SQLException {
		mDb.execSQL(getCreateTableCommandForName(mTableName, false));
	}

	public Collection<T> findAll() {
		try {
			Cursor c = selectAll();

			try {
				return fromCursor(c);
			} finally {
				c.close();
			}
		} catch (Exception e) {
			Log.exception(TAG, "findAll() failure", e);
			return null;
		}
	}

	public Collection<T> findAllWherePkLike(String pkPattern) {
		try {
			long startTime = System.currentTimeMillis();
			Cursor cursor = selectAllWherePkLike(pkPattern);
			Log.trace(TAG, String.format("selecting records from db took %d millis", System.currentTimeMillis() - startTime));

			try {
				return fromCursor(cursor);
			} finally {
				cursor.close();
			}
		} catch (Exception e) {
			Log.exception(TAG, "findAllWherePkLike(String) failure", e);
			return null;
		}
	}

	public T findOne(String id) {
		try {
			Cursor c = selectById(id);

			try {
				Iterator<T> iterator = fromCursor(c).iterator();

				return iterator.hasNext() ? iterator.next() : null;
			} finally {
				c.close();
			}
		} catch (Exception e) {
			Log.exception(TAG, "findOne() failure", e);

			return null;
		}
	}

	public Future<Iterable<T>> saveAsync(final Iterable<T> entities) {
		return sBackgroundExecutor.submit(new Callable<Iterable<T>>() {
			@Override
			public Iterable<T> call() throws Exception {
				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
				
				return save(entities);
			}
		});
	}

	public Future<T> saveAsync(final T entity) {
		return sBackgroundExecutor.submit(new Callable<T>() {
			@Override
			public T call() throws Exception {
				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

				return save(entity);
			}
		});
	}

	public Iterable<T> save(Iterable<T> entities) {
		ArrayList<T> results = new ArrayList<T>();

		for (T entity : entities) {
			String entityId = getEntityId(entity);

			try {
				byte[] serializationBytes = serializeEntity(entity);

				long ts = getEntityActualityTime(entity);

				replace(entityId, ts, serializationBytes);

				results.add(entity);
			} catch (Exception e) {
				Log.exception(TAG, "Unable to save entity into database", e);

				results.add(null);
			}
		}

		return results;
	}

	public T save(T entity) {
		ArrayList<T> entities = new ArrayList<T>();
		entities.add(entity);
		return save(entities).iterator().next();
	}

	public void deleteEntity(T entity) {
		deleteOne(getEntityId(entity));
	}

	public void deleteOne(String id) {
		try {
			synchronized (mDeleteById) {
				prepareDeleteByIdStatement(id).execute();	
			}
		} catch (Exception e) {
			Log.exception(TAG, "deleteOne() failure", e);
		}
	}

	public void deleteAll() {
		try {
			synchronized (mDeleteAll) {
				prepareDeleteAllStatement().execute();	
			}
		} catch (Exception e) {
			Log.exception(TAG, "deleteOne() failure", e);
		}
	}

	public boolean deleteAllWherePkLike(String pkPattern) {
		try {
			synchronized (mDeleteWherePkLike) {
				mDeleteWherePkLike.bindString(1, pkPattern);
				mDeleteWherePkLike.execute();				
			}

			return true;
		} catch (Exception e) {
			Log.exception(TAG, "deleteAll(long, long) failure", e);
			return false;
		}
	}

	public static String getCreateTableCommandForName(String tableName) {
		return getCreateTableCommandForName(tableName, false);
	}

	abstract protected String getEntityId(T entity);
	abstract protected long getEntityActualityTime(T entity);

	private static String getCreateTableCommandForName(String tableName, boolean ifNotExists) {
		return "create table " + (ifNotExists ? "if not exists " : "") + tableName + " (\n" +
				Columns._ID + " text primary key,\n" +
				Columns.ACTUALITY_TIME + " long,\n" +
				Columns.DATA + " blob\n" +				
				");";
	}

	Collection<T> fromCursor(Cursor c) {
		ArrayList<byte[]> blobs = new ArrayList<byte[]>();

		c.moveToFirst();

		while (!c.isAfterLast()) {
			int columnIndex = c.getColumnIndex(Columns.DATA);

			blobs.add(c.getBlob(columnIndex));

			c.moveToNext();
		}

		ArrayList<T> result = new ArrayList<T>();

		boolean outOfTime = false;

		try {
			final long deserializeStartTime = System.currentTimeMillis();

			outOfTime = false;

			for (int i = 0; i < blobs.size(); i++) {
				byte[] blob = blobs.get(i);

				if (outOfTime && i >= mMandatoryObjectsCountForDeserialization) break;

				try {
					result.add(deserializeEntity(blob));
				} catch (Exception e) {
					Log.exception(TAG, "Error deserializing object from bytes", e);
				}

				outOfTime = System.currentTimeMillis() - deserializeStartTime > mTimelimitForDeserialization;
			}

			Log.trace(TAG, String.format("deserialization of %d objects took %d millis (out of time = %b)", result.size(), System.currentTimeMillis() - deserializeStartTime, outOfTime));
		} catch (Exception e) {
			throw new RuntimeException("Should never happen", e);
		}

		return result;
	}

	Cursor selectById(String id) throws Exception {
		return new SQLiteSelectionCommand()
		.select(Columns._ID, Columns.ACTUALITY_TIME, Columns.DATA)
		.from(mTableName)
		.where(Columns._ID + "=?")
		.withValues(id)
		.orderByDescending(Columns.ACTUALITY_TIME)
		.execute(mDb);
	}

	Cursor selectAll() throws Exception {
		return new SQLiteSelectionCommand()
		.select(Columns._ID, Columns.ACTUALITY_TIME, Columns.DATA)
		.from(mTableName)
		.orderByDescending(Columns.ACTUALITY_TIME)
		.execute(mDb);
	}

	Cursor selectAllWherePkLike(String likeValue) throws Exception {
		return new SQLiteSelectionCommand()
		.select(Columns._ID, Columns.ACTUALITY_TIME, Columns.DATA)
		.from(mTableName)
		.where(Columns._ID + " LIKE ?")
		.orderByDescending(Columns.ACTUALITY_TIME)
		.withValues(likeValue + "%").execute(mDb);
	}

	void replace(String id, long timestamp, byte[] data) throws IOException {
		try {
			synchronized (mReplace) {
				prepareReplaceStatement(id, timestamp, data).execute();	
			}
		} catch (SQLException e) {
			throw new IOException(e.getMessage());
		}		
	}

	boolean exists(String id) throws Exception {
		Cursor c = new SQLiteSelectionCommand()
		.select(Columns._ID)
		.from(mTableName)
		.where(Columns._ID + "=?")
		.withValues(id)
		.execute(mDb);
		try {
			return (c.getCount() > 0);
		} finally {
			c.close();
		}
	}

	abstract protected byte[] serializeEntity(T entity) throws Exception;

	abstract protected T deserializeEntity(byte[] blob) throws Exception;

	private SQLiteStatement prepareReplaceStatement(String id, long timestamp, byte[] data) {
		mReplace.bindString(Replace.ID, id);
		mReplace.bindLong(Replace.TIME_STAMP, timestamp);
		mReplace.bindBlob(Replace.DATA, data);
		return mReplace;
	}

	private SQLiteStatement prepareDeleteByIdStatement(String id) {
		mDeleteById.bindString(DeleteById.ID, id);
		return mDeleteById;
	}

	private interface Replace { 
		int ID = 1;
		int TIME_STAMP = 2;
		int DATA = 3;
	}

	private String getReplaceCommand() {
		return " insert or replace into " + mTableName + " values (?, ?, ?);";
	}

	private String getDeleteAllCommand() {
		return " delete from " + mTableName + ";";
	}

	private SQLiteStatement prepareDeleteAllStatement() {
		return mDeleteAll;
	}

	private interface DeleteById {			
		int ID = 1;			
	}

	private String getDeleteByIdCommand() {
		return " delete from " + mTableName + " where " + Columns._ID + "=?;";
	}
}
