package com.gorecode.vk.adapter;

import java.util.Random;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.gorecode.vk.R;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.gorecode.vk.data.ObjectSubset;
import com.gorecode.vk.data.loaders.CollectionLoader;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.task.LongActionContext;
import com.gorecode.vk.view.LoaderLayout;
import com.uva.log.Log;
import com.uva.utilities.ObserverCollection;

public class LoaderAdapter<D> extends EndlessListAdapter {
	public static interface Callbacks<D> {
		public void onLoadBegin();
		public void onPreLoadComplete(Exception error, ObjectSubset<D> data);
		public void onLoadComplete(Exception error, ObjectSubset<D> data);
	}

	public static class EmptyCallbacks<D> implements Callbacks<D> {
		@Override
		public void onLoadBegin() { }
		@Override
		public void onPreLoadComplete(Exception error, ObjectSubset<D> data) { }
		@Override
		public void onLoadComplete(Exception error, ObjectSubset<D> data) { }
	}

	public static final int LOAD_STATE_IDLE = 0x0;
	public static final int LOAD_STATE_LOADING = 0x1;
	public static final int LOAD_STATE_FREEZE_CAUSE_ERROR = 0x2;

	private static final String TAG = "LoaderAdapter";

	private static final Handler handler = new Handler(Looper.getMainLooper());

	private final ObserverCollection<Callbacks<D>> callbacks = new ObserverCollection<Callbacks<D>>();

	private int loadState = LOAD_STATE_IDLE;
	private LoadMoreDataTask pendingLoadMoreDataTask;
	private boolean loadMoreDataOnPendingViewInflate = true;

	private final CollectionLoader<D> loader;

	public LoaderAdapter(Context context, ListAdapter wrapped, CollectionLoader<D> loader) {
		super(context, wrapped);

		Preconditions.checkNotNull(context);
		Preconditions.checkNotNull(loader);

		this.loader = loader;

		displayLoadStateInAdapter();
	}

	public void registerCallbacks(Callbacks<D> callback) {
		callbacks.add(callback);
	}

	public void unregisterCallbacks(Callbacks<D> callback) {
		callbacks.remove(callback);
	}

	public void setLoadMoreDataOnPendingViewInflate(boolean loadOnPendingViewInflateEnabled) {
		loadMoreDataOnPendingViewInflate = loadOnPendingViewInflateEnabled;
	}

	public int getLoadState() {
		return loadState;
	}

	public boolean abortLoad() {
		if (pendingLoadMoreDataTask == null) return false;

		if (pendingLoadMoreDataTask.abort()) {
			pendingLoadMoreDataTask = null;
	
			loadState = LOAD_STATE_IDLE;

			notifyDataSetChanged();

			return true;
		} else {
			return false;
		}
	}

	public boolean loadMoreData() {
		Log.trace(TAG, "Try loadMoreData()");

		if (pendingLoadMoreDataTask != null) {
			Log.trace(TAG, "Has pendingLoadMoreDataTask, returning false");
			return false;
		}

		Log.trace(TAG, "Executing LoadMoreDataTask");

		loadState = LOAD_STATE_LOADING;
		pendingLoadMoreDataTask = new LoadMoreDataTask(getContext());
		pendingLoadMoreDataTask.execute();

		return true;
	}

	public Callbacks<D> displayLoadStateInLoaderLayoutOnLoadingFromScratch(final LoaderLayout loaderLayout) {
		final int TAG_KEY = R.id.tag_data_source_loader;
		final int TAG_VALUE = new Random().nextInt();

		View retryView = loaderLayout.getErrorView().findViewById(R.id.retry);

		if (retryView != null) {
			retryView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					loadMoreData();
				}
			});
		}

		Callbacks<D> reaction = new EmptyCallbacks<D>() {
			@Override
			public void onLoadBegin() {
				if (!loaderLayout.getTag(TAG_KEY).equals(TAG_VALUE)) return;

				if (getWrappedAdapter().getCount() == 0) {
					loaderLayout.displayLoadView();
				} else {
					loaderLayout.displayContent();
				}
			}

			@Override
			public void onLoadComplete(Exception error, ObjectSubset<D> data) {
				if (!loaderLayout.getTag(TAG_KEY).equals(TAG_VALUE)) return;

				if (error != null && getWrappedAdapter().getCount() == 0) {
					loaderLayout.displayErrorView();
				} else {
					loaderLayout.displayContent();
				}
			}
		};

		loaderLayout.setTag(TAG_KEY, TAG_VALUE);

		switch (getLoadState()) {
		case LOAD_STATE_LOADING:
			reaction.onLoadBegin();
			break;
		default:
			reaction.onLoadComplete(null, null);
		}

		registerCallbacks(reaction);

		return reaction;
	}

	@Override
	protected View getErrorView(ViewGroup parent) {
		View errorView = super.getErrorView(parent);

		View retryView = errorView.findViewById(R.id.retry);

		if (retryView != null) {
			retryView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					loadMoreData();
				}
			});
		}

		return errorView;
	}

	@Override
	protected View getPendingView(ViewGroup parent) {
		Log.trace(TAG, "Loading item inside EndlessListAdapter is beign shown");

		handler.removeCallbacks(maybeLoadMoreDataRunnable);
		handler.post(maybeLoadMoreDataRunnable);

		return super.getPendingView(parent);
	}

	protected void publishResults(ObjectSubset<D> results) {
		;
	}

	private void displayLoadStateInAdapter() {
		// Visualize this Loader state in Endless Adapter. 
		callbacks.add(new EmptyCallbacks<D>() {
			@Override
			public void onLoadComplete(Exception error, ObjectSubset<D> data) {
				if (error != null) {
					setMixinDisplayment(EndlessListAdapter.DISPLAYMENT_ERROR);
				} else {
					setMixinDisplayment((data.hasMore && loadMoreDataOnPendingViewInflate) ? EndlessListAdapter.DISPLAYMENT_LOADING : EndlessListAdapter.DISPLAYMENT_NO_MIXIN);
				}
			}

			@Override
			public void onLoadBegin() {
				setMixinDisplayment(EndlessListAdapter.DISPLAYMENT_LOADING);
			}
		});
	}

	private void notifyOnLoadBegin() {
		callbacks.callForEach(new Function<Callbacks<D>, Void>() {
			@Override
			public Void apply(Callbacks<D> arg) {
				arg.onLoadBegin();

				return null;
			}
		});
	}

	private void notifyOnPreLoadComplete(final Exception error, final ObjectSubset<D> data) {
		callbacks.callForEach(new Function<Callbacks<D>, Void>() {
			@Override
			public Void apply(Callbacks<D> arg) {
				arg.onPreLoadComplete(error, data);
				return null;
			}
		});
	}

	private void notifyOnLoadComplete(final Exception error, final ObjectSubset<D> data) {
		callbacks.callForEach(new Function<Callbacks<D>, Void>() {
			@Override
			public Void apply(Callbacks<D> arg) {
				arg.onLoadComplete(error, data);
				return null;
			}
		});
	}

	private class LoadMoreDataTask extends LongAction<Void, ObjectSubset<D>> {
		public LoadMoreDataTask(Context context) {
			super(context);
		}

		@Override
		protected void onPreExecute() {
			Log.trace(TAG, "onPreExecute()");

			notifyOnLoadBegin();
		}

		@Override
		protected ObjectSubset<D> doInBackgroundOrThrow(Void params) throws Exception {
			return loader.loadMoreData();
		}

		@Override
		protected void onComplete(LongActionContext<Void, ObjectSubset<D>> executionResult) {
			Log.trace(TAG, "onPostComplete()");

			pendingLoadMoreDataTask = null;

			Exception error = executionResult.error;

			ObjectSubset<D> data = executionResult.result;

			notifyOnPreLoadComplete(error, data);

			if (error == null) {
				loadState = LOAD_STATE_IDLE;

				publishResults(data);
			} else {
				loadState = LOAD_STATE_FREEZE_CAUSE_ERROR;

				Log.exception(TAG, "Unable to load list chunk", error);
			}

			notifyOnLoadComplete(error, data);
		}
	}

	private final Runnable maybeLoadMoreDataRunnable = new Runnable() {
		@Override
		public void run() {
			if (loadMoreDataOnPendingViewInflate) {
				loadMoreData();
			}
		}
	};
}
