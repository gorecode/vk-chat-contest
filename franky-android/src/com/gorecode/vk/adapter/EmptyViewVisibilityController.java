package com.gorecode.vk.adapter;

import android.database.DataSetObserver;
import android.view.View;
import android.widget.Adapter;

import com.gorecode.vk.R;

public class EmptyViewVisibilityController extends DataSetObserver {
	private final View mView;
	private final Adapter mAdapter;

	public static void bind(Adapter adapter, View view) {
		EmptyViewVisibilityController controller = new EmptyViewVisibilityController(view, adapter);

		adapter.registerDataSetObserver(controller);

		view.setTag(R.id.tag_empty_view_controller, controller.toString());
	}

	private EmptyViewVisibilityController(View view, Adapter adapter) {
		mView = view;
		mAdapter = adapter;

		updateViews();
	}

	@Override
	public void onChanged() {
		if (toString().equals(mView.getTag(R.id.tag_empty_view_controller))) {
			updateViews();	
		} else {
			mAdapter.unregisterDataSetObserver(this);
		}
	}

	private void updateViews() {
		final int visibility = mAdapter.getCount() == 0 ? View.VISIBLE : View.GONE;
		
		mView.setVisibility(visibility);
	}
}
