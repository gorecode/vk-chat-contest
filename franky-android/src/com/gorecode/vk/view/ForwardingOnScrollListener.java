package com.gorecode.vk.view;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import com.uva.utilities.ObserverCollection;

public class ForwardingOnScrollListener implements OnScrollListener {
	private ObserverCollection<OnScrollListener> mListeners = new ObserverCollection<AbsListView.OnScrollListener>();

	public void addListener(OnScrollListener l) {
		mListeners.add(l);
	}

	public void removeListener(OnScrollListener l) {
		mListeners.remove(l);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		for (OnScrollListener l : mListeners) {
			l.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		for (OnScrollListener l : mListeners) {
			l.onScrollStateChanged(view, scrollState);
		}
	}
}
