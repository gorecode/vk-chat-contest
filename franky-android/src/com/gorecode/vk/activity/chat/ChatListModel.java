package com.gorecode.vk.activity.chat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.Context;

import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.collections.SortedTable;
import com.gorecode.vk.collections.Table;
import com.gorecode.vk.collections.TableChange;
import com.gorecode.vk.collections.TableChanges;
import com.gorecode.vk.collections.TableObserver;
import com.gorecode.vk.data.ChatMessage;
import com.uva.log.Log;

public class ChatListModel extends SortedTable<ChatListItem> implements TableObserver<ChatMessage> {
	private static final String TAG = ChatListModel.class.getSimpleName();

	private static final boolean DEBUG = true;

	private static final Calendar sCalendar1 = new GregorianCalendar();
	private static final Calendar sCalendar2 = new GregorianCalendar();

	private static final Comparator<ChatListItem> PENDING_FIRST = new Comparator<ChatListItem>() {
		@Override
		public int compare(ChatListItem object1, ChatListItem object2) {
			if (object1.dispatchState == object2.dispatchState) return 0;
			if (object1.dispatchState == ChatListItem.DISPATCH_STATE_PENDING) return 1;
			if (object2.dispatchState == ChatListItem.DISPATCH_STATE_PENDING) return -1;
			return 0;
		}
	};

	private static final Comparator<ChatListItem> BY_TIMELINE = new Comparator<ChatListItem>() {
		@Override
		public int compare(ChatListItem object1, ChatListItem object2) {
			if (object1.getTimestamp() > object2.getTimestamp()) return 1;
			if (object1.getTimestamp() < object2.getTimestamp()) return -1;
			return 0;
		}
	};

	private final Context mContext;

	private final ChatTable mMessages;

	public ChatListModel(Context context, ChatTable messages) {
		super(new Comparator<ChatListItem>() {
			@Override
			public int compare(ChatListItem object1, ChatListItem object2) {
				int c1 = PENDING_FIRST.compare(object1, object2);

				if (c1 == 0) {
					return BY_TIMELINE.compare(object1, object2);
				} else {
					return c1;
				}
			}
		});

		mContext = context;

		mMessages = messages;
		mMessages.registerObserver(this);

		setChangesCollectingEnabled(false);
	}

	@Override
	public long getIdOfObject(ChatListItem object) {
		return object.getId();
	}

	public ChatTable getMessages() {
		return mMessages;
	}

	public Collection<Long> getSelection() {
		ArrayList<Long> selection = new ArrayList<Long>();

		for (ChatListItem item : asList()) {
			if (item.isMarked) {
				selection.add(item.message.id);
			}
		}

		return selection;
	}

	public void resetSelection() {
		for (ChatListItem item : asList()) {
			item.isMarked = false;
		}

		notifyTableChanged();
	}

	public void setDispatchStateState(long mid, int dispatchState) {
		ChatListItem item = getById(mid);

		if (item == null) {
			return;
		}

		item.dispatchState = dispatchState;

		put(item);
	}

	public boolean isSelected(long mid) {
		ChatListItem item = getById(mid);

		if (item == null) {
			return false;
		}

		return item.isMarked;
	}

	public void setSelection(long mid, boolean selected) {
		ChatListItem item = getById(mid);

		if (item == null) {
			return;
		}

		item.isMarked = selected;

		put(item);

		notifyTableChanged();
	}

	private void onMessagesTableChanged(TableChanges<ChatMessage> changes) {
		boolean separatorsRebuiltNeedle = false;

		for (TableChange<ChatMessage> change : changes) {
			ChatMessage message = change.getValue();

			if (change.isValueDeleted()) {
				removeById(message.id);
			}

			if (change.isValuePut()) {
				ChatListItem item = getById(message.id);

				if (item == null) {
					item = ChatListItem.newMessage(message);

					separatorsRebuiltNeedle = true;
				}

				item.message = message;

				put(item);
			}
		}

		if (separatorsRebuiltNeedle) {
			if (DEBUG) Log.debug(TAG, "Rebuilding separators");

			List<ChatListItem> items = asList();

			for (int i = 0; i < items.size(); i++) {
				ChatListItem item = items.get(i);

				if (item.isSeparator()) {
					remove(item);

					if (i >= items.size()) {
						break;
					}
				}
			}

			ArrayList<ChatListItem> separatorsToAdd = new ArrayList<ChatListItem>();

			for (int i = 0; i < items.size() - 1; i++) {
				ChatListItem i1 = items.get(i);
				ChatListItem i2 = items.get(i + 1);

				final long time1 = i1.getTimestamp();
				final long time2 = i2.getTimestamp();

				if (isTimeForSeparator(time1, time2)) {
					if (DEBUG) Log.debug(TAG, String.format("Separator added, time = %s, i1 = %d / %s, i2 = %d / %s", new Date(i2.getTimestamp()), i1.message.id, i1.message.content.text, i2.message.id, i2.message.content.text));

					separatorsToAdd.add(ChatListItem.newSeparator(i2.message));
				}
			}

			putAll(separatorsToAdd);

			if (DEBUG) Log.debug(TAG, "Separators rebuilt finished");
		}

		notifyTableChanged();
	}

	private boolean isTimeForSeparator(long time1, long time2) {
		sCalendar1.setTimeInMillis(time1);
		sCalendar2.setTimeInMillis(time2);

		boolean sameDayOfMonth = sCalendar1.get(Calendar.DAY_OF_MONTH) == sCalendar2.get(Calendar.DAY_OF_MONTH);
		boolean sameMonth = sCalendar1.get(Calendar.MONTH) == sCalendar2.get(Calendar.MONTH);
		boolean sameYear = sCalendar1.get(Calendar.YEAR) == sCalendar2.get(Calendar.YEAR);
		boolean sameDate = sameDayOfMonth && sameMonth && sameYear;

		if (sameDate) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void onTableChanged(final Table<ChatMessage> source, final TableChanges<ChatMessage> changes) {
		VkApplication.from(mContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (source == mMessages) {
					onMessagesTableChanged(changes);
				}
			}
		});
	}
}
