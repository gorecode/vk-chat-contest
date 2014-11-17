package com.gorecode.vk.view;

import android.os.Handler;
import android.os.Looper;
import android.widget.AbsListView;

import com.uva.log.Log;

public class BetterScrollDetector implements AbsListView.OnScrollListener {
	public static interface OnScrollStoppedListener {
		public void onScrollStopped();
	}

	private static final String TAG = BetterScrollDetector.class.getSimpleName();

	private static final boolean DEBUG = false;

	private static final long SCROLL_TIMEOUT = 300;

	private final Handler mHandler = new Handler(Looper.getMainLooper());

	private int mScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

	private OnScrollStoppedListener mOnScrollStoppedListener;

	public int getScrollState() {
		return mScrollState;
	}

	public void notifyScrollStopped() {
		if (mOnScrollStoppedListener != null) {
			mOnScrollStoppedListener.onScrollStopped();
		}
	}

	public void setOnScrollStoppedListener(OnScrollStoppedListener onScrollListener) {
		mOnScrollStoppedListener = onScrollListener;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		mHandler.removeCallbacks(mSetListViewIsNotScrollingRunnable);

		mHandler.postDelayed(mSetListViewIsNotScrollingRunnable, SCROLL_TIMEOUT);

		if (mScrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
			if ((firstVisibleItem + visibleItemCount == totalItemCount) && (visibleItemCount != 0) && (totalItemCount != 0)) {
				setListViewIsNotScrolling();
			}

			if ((firstVisibleItem == 0) && (totalItemCount == 0)) {
				setListViewIsNotScrolling();
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (DEBUG) Log.debug(TAG, "Scroll state = " + scrollState);

		mScrollState = scrollState;

		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
			setListViewIsNotScrolling();
		}
	}

	private void setListViewIsNotScrolling() {
		mHandler.removeCallbacks(mSetListViewIsNotScrollingRunnable);

		if (DEBUG) Log.debug(TAG, "Scroll state is idle");

		notifyScrollStopped();

		mScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
	}

	private final Runnable mSetListViewIsNotScrollingRunnable = new Runnable() {
		@Override
		public void run() {
			setListViewIsNotScrolling();
		}
	};
}
