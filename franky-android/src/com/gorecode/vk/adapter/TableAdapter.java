package com.gorecode.vk.adapter;

import android.widget.BaseAdapter;

import com.gorecode.vk.collections.Table;
import com.gorecode.vk.collections.TableChanges;
import com.gorecode.vk.collections.TableObserver;

public abstract class TableAdapter<E> extends BaseAdapter implements TableObserver<E> {
	private final Table<E> mTable;

	public TableAdapter(Table<E> table) {
		super();

		mTable = table;
		mTable.registerObserver(this);
	}

	public Table<E> getTable() {
		return mTable;
	}
	
	@Override
	public int getCount() {
		return mTable.size();
	}

	@Override
	public E getItem(int position) {
		return mTable.asList().get(position);
	}

	@Override
	public long getItemId(int position) {
		return mTable.getIdOfObject(getItem(position));
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void onTableChanged(Table<E> source, TableChanges<E> changes) {
		if (source == mTable) {
			notifyDataSetChanged();
		}
	}
}
