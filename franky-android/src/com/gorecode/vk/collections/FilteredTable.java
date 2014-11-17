package com.gorecode.vk.collections;

import com.google.common.base.Predicate;

public class FilteredTable<T> extends TableReflection<T, T> {
	private final Predicate<T> mFilter;

	public FilteredTable(Table<T> source, Predicate<T> predicate) {
		super(source, null);

		mFilter = predicate;
	}

	@Override
	public long getIdOfObject(T object) {
		return getSource().getIdOfObject(object);
	}

	@Override
	protected void reflectChanges(TableChanges<T> changes) {
		for (TableChange<T> change : changes) {
			if (mFilter.apply(change.getValue())) {
				change.executeOnTable(this);
			}
		}
	}
}
