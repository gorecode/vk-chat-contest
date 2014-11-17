package com.gorecode.vk.cache;

import java.util.Iterator;

import android.database.sqlite.SQLiteDatabase;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.FastPackUnpack;
import com.gorecode.vk.event.LoggedOutEvent;

public class ChatCache extends SQLiteBlobObjectCache<ChatMessage> {
	public static final String TABLE_NAME = "chats";

	@Inject
	public ChatCache(SQLiteDatabase db) {
		super(db, TABLE_NAME);
	}

	public ChatMessage findOneByMid(long mid) {
		Iterator<ChatMessage> it = findByMid(mid).iterator();

		if (it.hasNext()) {
			return it.next();
		} else {
			return null;
		}
	}

	public Iterable<ChatMessage> findByMid(long mid) {
		return findAllWherePkLike("%|" + getKeyForMid(mid));
	}

	public Iterable<ChatMessage> findByCid(long cid) {
		return findAllWherePkLike(getKeyForCid(cid) + "|%");
	}

	public boolean deleteAll(long cid) {
		return deleteAllWherePkLike(getKeyForCid(cid) + "|%");
	}

	@Override
	public String getEntityId(ChatMessage entity) {
		String cidKey = getKeyForCid(entity.getCid());
		String midKey = getKeyForMid(entity.getMid());

		return String.format("%s|%s", cidKey, midKey);
	}

	public boolean deleteByMid(long mid) {
		return deleteAllWherePkLike("%|" + getKeyForMid(mid));
	}

	public static String getKeyForMid(long mid) {
		return String.format("mid:%d", mid);
	}

	public static String getKeyForCid(long cid) {
		return String.format("cid:%d", cid);
	}

	@Subscribe
	public void onUserLoggedOut(LoggedOutEvent event) {
		deleteAll();
	}

	@Override
	public long getEntityActualityTime(ChatMessage entity) {
		return entity.timestamp;
	}

	@Override
	protected ChatMessage deserializeEntity(byte[] blob) throws Exception {
		return FastPackUnpack.deserializeChatMessage(blob);
	}

	@Override
	protected byte[] serializeEntity(ChatMessage entity) throws Exception {
		return FastPackUnpack.serializeChatMessage(entity);
	}
}
