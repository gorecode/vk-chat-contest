package com.gorecode.vk.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ForwardingTable<E> implements Table<E> {
	private final Table<E> mDelegate;

	public ForwardingTable(Table<E> delegate) {
		mDelegate = delegate;
	}

	@Override
	public void registerObserver(TableObserver<E> observer) {
		mDelegate.registerObserver(observer);
	}

	@Override
	public void unregisterObserver(TableObserver<E> observer) {
		mDelegate.unregisterObserver(observer);
	}

	@Override
	public List<E> asList() {
		return mDelegate.asList();
	}

	@Override
	public E getById(long id) {
		return mDelegate.getById(id);
	}

	@Override
	public TableChange<E> put(E object) {
		return mDelegate.put(object);
	}

	@Override
	public Collection<TableChange<E>> putAll(Iterable<E> objects) {
		return mDelegate.putAll(objects);
	}

	@Override
	public TableChange<E> removeById(long id) {
		return mDelegate.removeById(id);
	}

	@Override
	public long getIdOfObject(E object) {
		return mDelegate.getIdOfObject(object);
	}


	@Override
	public int size() {
		return mDelegate.size();
	}

	@Override
	public void clear() {
		mDelegate.clear();
	}

	@Override
	public void notifyTableChanged() {
		mDelegate.notifyTableChanged();
	}

	@Override
	public void setChangesCollectingEnabled(boolean enabled) {
		mDelegate.setChangesCollectingEnabled(enabled);
	}

	@Override
	public int indexOf(E object) {
		return mDelegate.indexOf(object);
	}

	@Override
	public boolean add(E object) {
		return mDelegate.add(object);
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		return mDelegate.addAll(collection);
	}

	@Override
	public boolean contains(Object object) {
		return mDelegate.contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return mDelegate.containsAll(collection);
	}

	@Override
	public boolean isEmpty() {
		return mDelegate.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return mDelegate.iterator();
	}

	@Override
	public boolean remove(Object object) {
		return mDelegate.remove(mDelegate);
	}

	@Override
	public boolean removeAll(Collection<?> object) {
		return mDelegate.removeAll(object);
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return mDelegate.retainAll(collection);
	}

	@Override
	public Object[] toArray() {
		return mDelegate.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return mDelegate.toArray(array);
	}
}
