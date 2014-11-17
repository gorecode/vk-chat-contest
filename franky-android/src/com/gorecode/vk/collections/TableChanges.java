package com.gorecode.vk.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class TableChanges<V> implements Iterable<TableChange<V>> {
	private Collection<TableChange<V>> mChanges = new ArrayList<TableChange<V>>();

	private boolean mCollectingEnabled = true;

	public void setCollectingEnabled(boolean enabled) {
		mCollectingEnabled = enabled;
	}

	void clear() {
		mChanges.clear();
	}

	TableChange<V> put(int index, V value) {
		TableChange<V> change = new TableChange<V>(TableChange.EVENT_TYPE_UPDATE_OR_INSERT, index, value);

		if (mCollectingEnabled) {
			mChanges.add(change);
		}

		return change;
	}

	TableChange<V> remove(int index, V value) {
		TableChange<V> change = new TableChange<V>(TableChange.EVENT_TYPE_DELETE, index, value);

		if (mCollectingEnabled) {
			mChanges.add(change);
		}

		return change;
	}

	public Iterable<TableChange<V>> getByEventTypeMask(final int mask) {
		return Iterables.filter(mChanges, new Predicate<TableChange<V>>() {
			@Override
			public boolean apply(TableChange<V> arg) {
				return (mask & arg.getEventType()) != 0;
			}
		});
	}

	@Override
	public Iterator<TableChange<V>> iterator() {
		return mChanges.iterator();
	}
}
