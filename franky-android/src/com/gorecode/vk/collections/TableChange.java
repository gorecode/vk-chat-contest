package com.gorecode.vk.collections;

public class TableChange<T> {
	public static final int EVENT_TYPE_DELETE = 0x1;
	public static final int EVENT_TYPE_UPDATE_OR_INSERT = 0x2;

	private final int mEventType;
	private final int mIndex;
	private final T mValue;

	public TableChange(int eventType, int index, T value) {
		mEventType = eventType;
		mIndex = index;
		mValue = value;
	}

	public void executeOnTable(Table<T> table) {
		if (isValueDeleted()) {
			table.removeById(table.getIdOfObject(getValue()));
		}
		if (isValuePut()) {
			table.put(getValue());
		}
	}

	public int getIndex() {
		return mIndex;
	}

	public int getEventType() {
		return mEventType;
	}

	public T getValue() {
		return mValue;
	}

	public boolean isValueDeleted() {
		return mEventType == EVENT_TYPE_DELETE;
	}

	public boolean isValuePut() {
		return mEventType == EVENT_TYPE_UPDATE_OR_INSERT;
	}
}
