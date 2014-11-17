package com.gorecode.vk.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.uva.utilities.ObserverCollection;

public abstract class SortedTable<T> implements Table<T> {
	private static final boolean DEBUG = false;

	private final TableChanges<T> mChanges = new TableChanges<T>();

	private final ObserverCollection<TableObserver<T>> mObservers = new ObserverCollection<TableObserver<T>>();

	private final Comparator<T> mOrdering;
	private final ArrayList<T> mList = new ArrayList<T>();
	private final TreeMap<Long, T> mMap = new TreeMap<Long, T>();

	public SortedTable(Comparator<T> ordering) {
		Preconditions.checkNotNull(ordering);

		mOrdering = ordering;
	}

	@Override
	public void registerObserver(TableObserver<T> observer) {
		mObservers.add(observer);
	}

	@Override
	public void unregisterObserver(TableObserver<T> observer) {
		mObservers.remove(observer);
	}

	@Override
	public Collection<TableChange<T>> putAll(Iterable<T> content) {
		ArrayList<TableChange<T>> changes = new ArrayList<TableChange<T>>();

		for (T each : content) {
			changes.add(put(each));
		}

		return changes;
	}

	public Comparator<T> getOrdering() {
		return mOrdering;
	}

	public Collection<TableChange<T>> putAll(T[] content) {
		return putAll(Arrays.asList(content));
	}

	@Override
	public TableChange<T> put(T object) {
		long id = getIdOfObject(object);

		T oldValue = mMap.get(id);

		if (oldValue != null && mOrdering.compare(oldValue, object) != 0) {
			removeById(id);
		}

		int indexOfBinarySearch = Collections.binarySearch(mList, object, mOrdering);
		int indexOfObject = -1;

		if (indexOfBinarySearch >= 0) {
			indexOfObject = searchIndexByIdNearby(id, indexOfBinarySearch);
		}

		if (indexOfObject >= 0) {
			mList.set(indexOfObject, object);
		} else {
			if (indexOfBinarySearch < 0) {
				indexOfObject = -indexOfBinarySearch - 1;
			} else {
				indexOfObject = indexOfBinarySearch;
			}

			mList.add(indexOfObject, object);
		}

		mMap.put(id, object);

		assertMapAndListAreSameSize();

		return mChanges.put(indexOfObject, object);
	}

	@Override
	public int indexOf(T object) {
		int index = Collections.binarySearch(mList, object, mOrdering);

		if (index >= 0) {
			return searchIndexByIdNearby(getIdOfObject(object), index);
		} else {
			return -1;
		}
	}

	private int searchIndexByIdNearby(long id, int index) {
		// Search forward.
		for (int i = index; i < mList.size(); i++) {
			if (getIdOfObject(mList.get(i)) == id) {
				return i;
			}
			if (mOrdering.compare(mList.get(index), mList.get(i)) != 0) {
				break;
			}
		}

		// Search backward.
		for (int i = index - 1; i >= 0; i--) {
			if (getIdOfObject(mList.get(i)) == id) {
				return i;
			}
			if (mOrdering.compare(mList.get(index), mList.get(i)) != 0) {
				break;
			}
		}

		return -1;
	}

	@Override
	public List<T> asList() {
		return mList;
	}

	@Override
	public int size() {
		return mMap.size();
	}

	public boolean containsKey(long key) {
		return getById(key) != null;
	}

	@Override
	public T getById(long id) {
		return mMap.get(id);
	}

	@Override
	public TableChange<T> removeById(long id) {
		T removed = mMap.remove(id);

		if (removed == null) {
			return null;
		}

		int index = indexOf(removed);

		if (index >= 0) {
			TableChange<T> change = mChanges.remove(index, removed);

			mList.remove(index);

			assertMapAndListAreSameSize();

			return change;
		} else {
			return null;
		}
	}

	@Override
	public void clear() {
		for (T value : mList) {
			mChanges.remove(0, value);
		}

		mList.clear();
		mMap.clear();
	}

	@Override
	public void setChangesCollectingEnabled(boolean enabled) {
		mChanges.setCollectingEnabled(enabled);
	}

	@Override
	public void notifyTableChanged() {
		Enumeration<TableObserver<T>> e = mObservers.toEnumeration();

		while (e.hasMoreElements()) {
			e.nextElement().onTableChanged(this, mChanges);
		}

		mChanges.clear();
	}


	@Override
	public boolean add(T object) {
		put(object).isValuePut();
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends T> collection) {
		putAll((Collection<T>)collection);

		return true;
	}

	@Override
	public boolean contains(Object object) {
		return asList().contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return asList().containsAll(collection);
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Iterator<T> iterator() {
		return asList().iterator();
	}

	@Override
	public boolean remove(Object object) {
		try {
			@SuppressWarnings("unchecked")
			T element = (T)object;

			return removeById(getIdOfObject(element)).isValueDeleted();
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> objects) {
		boolean changed = false;
		for (Object o : objects) {
			changed = changed | remove(o);
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return false;
	}

	@Override
	public Object[] toArray() {
		return asList().toArray();
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] array) {
		return asList().toArray(array);
	}

	private void assertMapAndListAreSameSize() {
		if (mMap.size() != mList.size() && DEBUG) {
			throw new RuntimeException("Inner map and list size must be equal");
		}
	}
}
