package com.gorecode.vk.collections;

import java.util.Comparator;

public abstract class TableReflection<F, T> extends SortedTable<T> implements TableObserver<F> {
	private final Table<F> mSource;

	public TableReflection(Table<F> source, Comparator<T> comparator) {
		super(comparator);

		mSource = source;
		mSource.registerObserver(this);

		sync();
	}

	@Override
	public void onTableChanged(Table<F> source, TableChanges<F> changes) {
		if (source == mSource) {
			reflectChanges(changes);

			notifyTableChanged();
		}
	}

	public Table<F> getSource() {
		return mSource;
	}

	abstract protected void reflectChanges(TableChanges<F> changes);

	protected void sync() {
		TableChanges<F> changes = new TableChanges<F>();

		int i = 0;

		for (F value : mSource.asList()) {
			changes.put(i++, value);
		}

		reflectChanges(changes);

		notifyTableChanged();
	}
}
