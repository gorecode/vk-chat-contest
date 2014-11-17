package com.gorecode.vk.adapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.widget.BaseAdapter;

public abstract class ListAdapter<T> extends BaseAdapter {
	private final List<T> mList;

	public ListAdapter(List<T> list) {
		this.mList = list;
	}

	public void replaceAll(Collection<T> items) {
		mList.clear();
		mList.addAll(items);
		notifyDataSetChanged();
	}

	public void replaceAll(T[] content) {
		replaceAll(Arrays.asList(content));
	}

	public List<T> getList() {
		return mList;
	}
	
	public void add(int index, T item) {
		mList.add(index, item);

		notifyDataSetChanged();		
	}

	public void add(T item) {
		mList.add(item);

		notifyDataSetChanged();
	}
	
	public void addAll(Collection<T> items) {
		mList.addAll(items);

		notifyDataSetChanged();
	}
	
	public void remove(T item) {
		mList.remove(item);

		notifyDataSetChanged();
	}
	
	public void clear() {
		mList.clear();

		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		synchronized (mList) {
			return mList.size();				
		}
	}

	@Override
	public T getItem(int position) {
		synchronized (mList) {
			return mList.get(position);
		}
	}
}