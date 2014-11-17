package com.gorecode.vk.activity.friends;

import java.util.concurrent.Future;

import roboguice.RoboGuice;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.VisibleInjectionContext;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.adapter.EmptyViewVisibilityController;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.imageloader.ListImageLoader;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.view.LoaderLayout;
import com.gorecode.vk.view.SearchView;
import com.uva.log.Log;

public class FriendsPageView extends LinearLayout implements OnItemClickListener, SearchView.OnQueryTextListener {
	private static final String TAG = FriendsPageView.class.getSimpleName();

	public static interface OnFriendItemClickListener {
		public void onFriendItemClick(AdapterView<?> adapterView, View view, int position, long id, Profile friend);
	}

	private LoaderLayout mLoaderLayout;
	private SearchView mSearchBar;
	private ListView mFriendsListView;

	private boolean mIsHidden;

	@Inject
	private ListImageLoader mImageLoader;
	@Inject
	private FriendsModel mFriendsModel;

	private OnFriendItemClickListener mOnFriendItemClickListener;

	private FriendListAdapter mFriendsListAdapter;

	private LongAction<Void, Void> mPendingSync;

	public FriendsPageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		inflate(context, R.layout.friends_page, this);

		if (!isInEditMode()) {
			RoboGuice.getInjector(context).injectMembersWithoutViews(this);

			mLoaderLayout = (LoaderLayout)findViewById(R.id.loaderLayout);

			mFriendsListAdapter = new FriendListAdapter(getContext(), mImageLoader, mFriendsModel) {
				@Override
				public void onModelChanged(FriendsModel model) {
					if (mIsHidden) {
						Log.debug(TAG, "skip filter, view is not visible");
					} else {
						super.onModelChanged(model);
					}
				}
			};

			EmptyViewVisibilityController.bind(mFriendsListAdapter, findViewById(android.R.id.empty));

			mSearchBar = new SearchView(getContext());
			mSearchBar.setOnQueryTextListener(this);

			mFriendsListView = (ListView)findViewById(android.R.id.list);
			mFriendsListView.setDrawingCacheEnabled(false);
			mFriendsListView.addHeaderView(mSearchBar);
			mFriendsListView.setAdapter(mFriendsListAdapter);
			mFriendsListView.setOnScrollListener(mImageLoader);
			mFriendsListView.setOnItemClickListener(this);

			Aibolit.doInjections(this, new VisibleInjectionContext(this));

			getFriendsAsync();
		}
	}

	public void onHiddenChanged(boolean hidden) {
		Log.debug(TAG, "onHiddenChanged(" + hidden + ")");

		mIsHidden = hidden;

		if (!hidden && mPendingSync == null) {
			mFriendsListAdapter.onModelChanged(mFriendsModel);
		}
	}

	public void setOnFriendItemClickListener(OnFriendItemClickListener listener) {
		mOnFriendItemClickListener = listener;
	}

	public void destroy() {
		if (mPendingSync != null) {
			mPendingSync.abort();
		}

		mImageLoader.stop();

		mFriendsModel.removeOnModelChangedListener(mFriendsListAdapter);
	}

	public void filterByAvailability(boolean onlineOnly) {
		mFriendsListAdapter.filterByAvailability(onlineOnly);
	}

	public Profile getFriendAtPosition(int position) {
		return mFriendsListAdapter.getItem(position).getFriend();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position >= mFriendsListView.getHeaderViewsCount()) {
			int friendPosition = position - mFriendsListView.getHeaderViewsCount();

			if (mOnFriendItemClickListener != null) {
				mOnFriendItemClickListener.onFriendItemClick(parent, view, position, id, mFriendsListAdapter.getItem(friendPosition).getFriend());
			}
		}
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		mFriendsListAdapter.filterByQuery(query);

		return true;
	}

	@Override
	public boolean onQueryTextChange(String query) {
		mFriendsListAdapter.filterByQuery(query);

		return true;
	}

	@InjectOnClickListener(R.id.retry)
	protected void onRetryButtonClicked(View view) {
		waitForSync(mFriendsModel.sync(getContext()));
	}

	protected void onSyncComplete() {
		mPendingSync = null;

		mFriendsModel.addOnModelChangedListener(mFriendsListAdapter);

		mFriendsListAdapter.reset();
	}

	private void waitForSync(final Future<?> syncTask) {
		if (mPendingSync != null) {
			mPendingSync.abort();
		}
		mPendingSync = new LongAction<Void, Void>(getContext()) {
			@Override
			protected Void doInBackgroundOrThrow(Void params) throws Exception {
				syncTask.get();

				return null;
			}

			@Override
			protected void onSuccess(Void unused) {
				onSyncComplete();
			}
		};
		mPendingSync.wrapWithSpinner(mLoaderLayout);
		mPendingSync.execute();
	}

	private void getFriendsAsync() {
		if (mFriendsModel.getFriends().size() > 0) {
			onSyncComplete();

			mFriendsModel.sync(getContext());
		} else {
			boolean lastSyncWasOk = false;

			Future<FriendsModel.SyncResult> lastSync = mFriendsModel.getLastSync();

			try {
				if (lastSync != null && lastSync.isDone()) {
					lastSync.get();
					lastSyncWasOk = true;
				}
			} catch (Exception e) {
				lastSyncWasOk = false;
			}

			if (lastSyncWasOk) {
				onSyncComplete();

				mFriendsModel.sync(getContext());
			} else {
				if (lastSync != null) {
					waitForSync(lastSync);
				} else {
					waitForSync(mFriendsModel.sync(getContext()));
				}
			}
		}
	}
}
