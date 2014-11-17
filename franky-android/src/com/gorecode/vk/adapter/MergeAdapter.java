/***
  Copyright (c) 2008-2009 CommonsWare, LLC
  Portions (c) 2009 Google, Inc.

  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */    

package com.gorecode.vk.adapter;

import java.util.ArrayList;
import java.util.Collection;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

/**
 * Adapter that merges multiple child adapters and views
 * into a single contiguous whole.
 *
 * Adapters used as pieces within MergeAdapter must
 * have view type IDs monotonically increasing from 0. Ideally,
 * adapters also have distinct ranges for their row ids, as
 * returned by getItemId().
 *
 */
public class MergeAdapter extends BaseAdapter {
	protected ArrayList<ListAdapter> pieces=new ArrayList<ListAdapter>();

	private final CascadeDataSetObserver cascadeDataSetObserver = new CascadeDataSetObserver();

	/**
	 * Stock constructor, simply chaining to the superclass.
	 */
	public MergeAdapter() {
		super();
	}

	public MergeAdapter(Collection<ListAdapter> adapters) {
		super();

		for (ListAdapter adapter : adapters) {
			addAdapter(adapter);
		}
	}

	/**
	 * Adds a new adapter to the roster of things to appear
	 * in the aggregate list.
	 * @param adapter Source for row views for this section
	 */
	public void addAdapter(ListAdapter adapter) {
		pieces.add(adapter);

		adapter.registerDataSetObserver(cascadeDataSetObserver);
	}

	public void removeAdapter(ListAdapter adapter) {
		pieces.remove(adapter);

		adapter.unregisterDataSetObserver(cascadeDataSetObserver);
	}

	/**
	 * Get the data item associated with the specified
	 * position in the data set.
	 * @param position Position of the item whose data we want
	 */
	@Override
	public Object getItem(int position) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				return(piece.getItem(position));
			}

			position-=size;
		}

		return(null);
	}

	/**
	 * Get the adapter associated with the specified
	 * position in the data set.
	 * @param position Position of the item whose adapter we want
	 */
	public ListAdapter getAdapter(int position) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				return(piece);
			}

			position-=size;
		}

		return(null);
	}

	/**
	 * How many items are in the data set represented by this
	 * Adapter.
	 */
	@Override
	public int getCount() {
		int total=0;

		for (ListAdapter piece : pieces) {
			total+=piece.getCount();
		}

		return(total);
	}

	/**
	 * Returns the number of types of Views that will be
	 * created by getView().
	 */
	@Override
	public int getViewTypeCount() {
		int total=0;

		for (ListAdapter piece : pieces) {
			total+=piece.getViewTypeCount();
		}

		return(Math.max(total, 1));   // needed for setListAdapter() before content add'
	}

	/**
	 * Get the type of View that will be created by getView()
	 * for the specified item.
	 * @param position Position of the item whose data we want
	 */
	@Override
	public int getItemViewType(int position) {
		int typeOffset=0;
		int result=-1;

		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				int itemViewType = piece.getItemViewType(position);

				if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
					result = IGNORE_ITEM_VIEW_TYPE;
					break;
				}

				result=typeOffset+itemViewType;

				break;
			}

			position-=size;
			typeOffset+=piece.getViewTypeCount();
		}

		return(result);
	}

	/**
	 * Are all items in this ListAdapter enabled? If yes it
	 * means all items are selectable and clickable.
	 */
	@Override
	public boolean areAllItemsEnabled() {
		return(false);
	}

	/**
	 * Returns true if the item at the specified position is
	 * not a separator.
	 * @param position Position of the item whose data we want
	 */
	@Override
	public boolean isEnabled(int position) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				return(piece.isEnabled(position));
			}

			position-=size;
		}

		return(false);
	}

	/**
	 * Get a View that displays the data at the specified
	 * position in the data set.
	 * @param position Position of the item whose data we want
	 * @param convertView View to recycle, if not null
	 * @param parent ViewGroup containing the returned View
	 */
	@Override
	public View getView(int position, View convertView,
			ViewGroup parent) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {

				return(piece.getView(position, convertView, parent));
			}

			position-=size;
		}

		return(null);
	}

	/**
	 * Get the row id associated with the specified position
	 * in the list.
	 * @param position Position of the item whose data we want
	 */
	@Override
	public long getItemId(int position) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				return(piece.getItemId(position));
			}

			position-=size;
		}

		return(-1);
	}

	private class CascadeDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			notifyDataSetInvalidated();
		}
	}
}