package com.gorecode.vk.collections;

import java.util.Collection;
import java.util.List;

public interface Table<E> extends Collection<E> {
	public long getIdOfObject(E object);
	public E getById(long id);
	public TableChange<E> put(E object);
	public Collection<TableChange<E>> putAll(Iterable<E> objects);
	public TableChange<E> removeById(long id);
	public int size();
	public void clear();
	public int indexOf(E object);
	public List<E> asList();

	public void registerObserver(TableObserver<E> observer);
	public void unregisterObserver(TableObserver<E> observer);
	public void notifyTableChanged();
	public void setChangesCollectingEnabled(boolean enabled);
}
