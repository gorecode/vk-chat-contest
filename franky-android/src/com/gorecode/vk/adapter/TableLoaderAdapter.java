package com.gorecode.vk.adapter;

import java.util.Arrays;

import android.content.Context;
import android.widget.ListAdapter;

import com.google.common.base.Preconditions;
import com.gorecode.vk.collections.Table;
import com.gorecode.vk.data.ObjectSubset;
import com.gorecode.vk.data.loaders.CollectionLoader;

public class TableLoaderAdapter<D> extends LoaderAdapter<D> {
	private final Table<D> dataSource;

	public TableLoaderAdapter(Context context, CollectionLoader<D> loader, Table<D> dataSource, ListAdapter wrapped) {
		this(context, loader, dataSource, wrapped, true);
	}

	public TableLoaderAdapter(Context context, CollectionLoader<D> loader, Table<D> dataSource, ListAdapter wrapped, boolean forceLoad) {
		super(context, wrapped, loader);

		Preconditions.checkNotNull(context);
		Preconditions.checkNotNull(dataSource);
		Preconditions.checkNotNull(loader);

		this.dataSource = dataSource;

		if (forceLoad) {
			setMixinDisplayment(DISPLAYMENT_LOADING);
		}
	}

	public Table<D> getDataSource() {
		return dataSource;
	}

	@Override
	protected void publishResults(ObjectSubset<D> data) {
		dataSource.putAll(Arrays.asList(data.content));
		dataSource.notifyTableChanged();
	}
}
