package com.gorecode.vk.adapter;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListAdapter;

import com.gorecode.vk.R;
import com.gorecode.vk.view.UserSeparatorListItemViewController;

public class Adapters {
	public static ListAdapter newSectionAdapter(Context context, final ListAdapter contentAdapter, int textResId) {
		View view = LayoutInflater.from(context).inflate(R.layout.user_list_item_separator, null);
		UserSeparatorListItemViewController.bind(view).setText(context.getString(textResId));
		return newSectionAdapter(contentAdapter, view);
	}

	public static ListAdapter newSectionAdapter(final ListAdapter contentAdapter, final View headerView) {
		final ArrayList<View> views = new ArrayList<View>(Collections.singleton(headerView));

		final SackOfViewsAdapter headerAdapter = new SackOfViewsAdapter(views) {
			@Override
			public int getCount() {
				if (contentAdapter.getCount() > 0) return 1;

				return 0;
			}

			@Override
			public int getViewTypeCount() {
				return 1;
			}
		};
		
		final MergeAdapter sectionAdapter = new MergeAdapter();

		sectionAdapter.addAdapter(headerAdapter);
		sectionAdapter.addAdapter(contentAdapter);

		return sectionAdapter;
	}

	private Adapters() {
		;
	}
}
