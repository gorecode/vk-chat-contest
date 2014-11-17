package com.gorecode.vk.collections;

public interface TableObserver<E> {
	public void onTableChanged(Table<E> source, TableChanges<E> changes);
}
