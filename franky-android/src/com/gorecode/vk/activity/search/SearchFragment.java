package com.gorecode.vk.activity.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.VisibleInjectionContext;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.gorecode.vk.activity.UserActivity;
import com.gorecode.vk.activity.VkFragment;
import com.gorecode.vk.activity.friends.FriendsModel;
import com.gorecode.vk.adapter.Adapters;
import com.gorecode.vk.adapter.EmptyViewVisibilityController;
import com.gorecode.vk.adapter.LoaderAdapter;
import com.gorecode.vk.adapter.MergeAdapter;
import com.gorecode.vk.adapter.UserListAdapter;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.ObjectSubset;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.UserSearch;
import com.gorecode.vk.data.loaders.BackwardPreloader;
import com.gorecode.vk.data.loaders.CollectionLoader;
import com.gorecode.vk.imageloader.ListImageLoader;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.utilities.Toasts;
import com.gorecode.vk.view.LoaderLayout;
import com.gorecode.vk.view.SearchView;
import com.uva.lang.ThreadFactories;
import com.uva.log.Log;

public class SearchFragment extends VkFragment implements SearchView.OnQueryTextListener, OnItemClickListener {
	private static final String TAG = SearchFragment.class.getSimpleName();

	private static final ExecutorService sExecutor = new ThreadPoolExecutor(0, 2, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), ThreadFactories.WITH_LOWEST_PRIORITY);

	private Context mContext;

	@InjectView(R.id.content)
	private LoaderLayout mLoaderLayout;
	@InjectView(R.id.suggestionsLayout)
	private View mSuggestionsLayout;
	@InjectView(R.id.searchLayout)
	private View mSearchLayout;
	@InjectView(android.R.id.list)
	private ListView mListView;

	private SearchView mSearchView;

	private View mSearchHintView;

	@Inject
	private ListImageLoader mImageLoader;
	@Inject
	private FriendsModel mFriendsModel;
	@Inject
	private FriendSuggestionsModel mSuggestionsModel;

	@Inject
	private VkModel mVk;

	public SearchAdapter mSearchListAdapter;
	public SearchAdapter mSuggestionsListAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_fragment, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		mContext = getActivity();

		setUpViews();

		mSuggestionsModel.addOnModelChangedListener(mSuggestionsListAdapter);

		mFriendsModel.addOnModelChangedListener(mSuggestionsListAdapter);

		syncSuggestions();
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.debug(TAG, "onResume()");
	}

	@Override
	public void onPause() {
		super.onPause();

		Log.debug(TAG, "onPause()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mImageLoader.stop();

		mSuggestionsModel.removeOnModelChangedListener(mSuggestionsListAdapter);

		mFriendsModel.removeOnModelChangedListener(mSuggestionsListAdapter);

		detachSearchListAdapterFromModel();
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		Log.debug(TAG, "onHidden(" + hidden + ")");

		if (!hidden) {
			mSuggestionsListAdapter.syncWithModel();

			if (mSearchListAdapter != null) {
				mSearchListAdapter.syncWithModel();
			}		
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.activity_friend_suggestions, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.menu_reject_all_offers).setVisible(mSuggestionsModel.getOffers().size() > 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean handled = super.onOptionsItemSelected(item);

		if (handled) return true;

		if (item.getItemId() == R.id.menu_reject_all_offers) {
			onRejectAllOffersAction();
			return true;
		}

		return false;
	}

	@Override
	public boolean onQueryTextChange(String query) {
		filterByQuery(query);

		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		filterByQuery(query);

		return true;
	}

	@InjectOnClickListener(R.id.retry)
	public void onRetryButtonClick(View v) {
		syncSuggestions();
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Object item = adapterView.getItemAtPosition(position);

		if (item != null && item instanceof Profile) {
			Profile user = (Profile)item;

			int userCategory = UserActivity.USER_CATEGORY_STRANGER;

			if (mFriendsModel.getFriendsSet().containsKey(user.id)) {
				userCategory = UserActivity.USER_CATEGORY_FRIEND;
			} else {
				boolean isOffer = false;

				for (Profile offer : mSuggestionsModel.getOffers()) {
					if (offer.id == user.id) {
						isOffer = true;
						break;
					}
				}

				if (isOffer) {
					userCategory = UserActivity.USER_CATEGORY_OFFER;
				}
			}

			Intent intent = UserActivity.getDisplayIntent(mContext, user, userCategory);

			startActivity(intent);
		}
	}

	private void onRejectAllOffersAction() {
		LongAction<?, ?> action = new LongAction<Void, Void>(getActivity(), SearchFragment.this) {
			@Override
			protected Void doInBackgroundOrThrow(Void params) throws Exception {
				mVk.rejectAllFriendshipOffers();

				return null;
			}

			@Override
			protected void onSuccess(Void unused) {
				Toasts.makeText(mContext, R.string.toast_friendship_offers_rejected).show();
			}
		};
		action.execute();
	}

	private void syncSuggestions() {
		boolean hasDataToShow = (mSuggestionsModel.getOffers().size() + mSuggestionsModel.getSuggestions().size() > 0);

		final Future<?> syncFuture = mSuggestionsModel.sync(mContext);

		if (!hasDataToShow) {
			LongAction<Void, Void> waitForSyncAction = new LongAction<Void, Void>(mContext, this) {
				@Override
				protected Void doInBackgroundOrThrow(Void params) throws Exception {
					syncFuture.get();

					return null;
				}
			};
			waitForSyncAction.wrapWithSpinner(mLoaderLayout);
			waitForSyncAction.execute();
		}
	}

	private void filterByQuery(String query) {
		detachSearchListAdapterFromModel();

		if (Strings.isNullOrEmpty(query)) {
			mSearchLayout.setVisibility(View.GONE);
			mSuggestionsLayout.setVisibility(View.VISIBLE);
			mSearchHintView.setVisibility(View.VISIBLE);

			mListView.setAdapter(mSuggestionsListAdapter);
		} else {
			if (mSearchListAdapter != null) {
				mSearchListAdapter.abortSearch();
			}

			mSearchListAdapter = new SearchAdapter(query);

			mFriendsModel.addOnModelChangedListener(mSearchListAdapter);
			mSuggestionsModel.addOnModelChangedListener(mSearchListAdapter);

			mSearchLayout.setVisibility(View.VISIBLE);
			mSuggestionsLayout.setVisibility(View.GONE);
			mSearchHintView.setVisibility(View.GONE);

			mListView.setAdapter(mSearchListAdapter);

			EmptyViewVisibilityController.bind(mSearchListAdapter, mSearchLayout.findViewById(android.R.id.empty));
		}
	}

	private void setUpViews() {
		mListView.setOnItemClickListener(this);

		mSuggestionsListAdapter = new SearchAdapter();

		mSearchView = new SearchView(getActivity());
		mSearchView.setBackgroundResource(0);
		mSearchView.getQueryEdit().setHint(R.string.search_hint_people);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.findViewById(R.id.content_layout).setBackgroundResource(0);

		mSearchHintView = LayoutInflater.from(mContext).inflate(R.layout.header_people_search_hint, null);

		mListView.setDrawingCacheEnabled(false);
		mListView.addHeaderView(mSearchView);
		mListView.addHeaderView(mSearchHintView);
		mListView.setAdapter(mSuggestionsListAdapter);

		// XXX: Hack to hide header view.
		mSearchHintView = mSearchHintView.findViewById(R.id.text);

		EmptyViewVisibilityController.bind(mSuggestionsListAdapter, mSuggestionsLayout.findViewById(android.R.id.empty));

		Aibolit.doInjections(this, new VisibleInjectionContext(getView()));
	}

	private void detachSearchListAdapterFromModel() {
		if (mSearchListAdapter != null) {
			mFriendsModel.removeOnModelChangedListener(mSearchListAdapter);
			mSuggestionsModel.removeOnModelChangedListener(mSearchListAdapter);
		}
	}

	private class SearchAdapter extends MergeAdapter implements FriendSuggestionsModel.OnModelChangedListener, FriendsModel.OnModelChangedListener {
		/**
		 * Unfiltered data.
		 */

		private final List<Profile> mAllOffers = new ArrayList<Profile>();
		private final List<Profile> mAllSuggestions = new ArrayList<Profile>();
		private final List<Profile> mAllFriends = new ArrayList<Profile>();

		/**
		 * Data filtered by query.
		 */

		private final List<Profile> mOffers = new ArrayList<Profile>();
		private final List<Profile> mSuggestions = new ArrayList<Profile>();
		private final List<Profile> mFriends = new ArrayList<Profile>();

		/**
		 * Adapters for sections in list.
		 */

		private final UserListAdapter mFriendsAdapter = new UserListAdapter(mContext, mImageLoader, mFriends);
		private final UserListAdapter mOffersAdapter = new UserListAdapter(mContext, mImageLoader, mOffers);
		private final UserListAdapter mSuggestionsAdapter = new UserListAdapter(mContext, mImageLoader, mSuggestions);

		private final GlobalSearchAdapter mGlobalSearchAdapter;

		/**
		 * Other things.
		 */

		private final FilterByQuery mFilter = new FilterByQuery();

		private final String mQuery;

		public SearchAdapter(String query) {
			mQuery = query;

			resetUnfilteredItems();

			addAdapter(Adapters.newSectionAdapter(mContext, mOffersAdapter, R.string.list_item_separator_offers));
			addAdapter(Adapters.newSectionAdapter(mContext, mSuggestionsAdapter, R.string.list_item_separator_possible_friends));
			addAdapter(Adapters.newSectionAdapter(mContext, mFriendsAdapter, R.string.list_item_separator_friends));

			if (Strings.isNullOrEmpty(query)) {
				mGlobalSearchAdapter = null;
			} else {
				List<Profile> searchResults = new ArrayList<Profile>();

				ListAdapter searcherAdapter = Adapters.newSectionAdapter(mContext, new UserListAdapter(mContext, mImageLoader, searchResults), R.string.list_item_separator_all_people);

				mGlobalSearchAdapter = new GlobalSearchAdapter(mContext, searcherAdapter, searchResults, query);
				mGlobalSearchAdapter.loadMoreData();

				addAdapter(mGlobalSearchAdapter);
			}

			filterItems();
		}

		public SearchAdapter() {
			this("");
		}

		public void syncWithModel() {
			resetUnfilteredItems();

			filterItems();
		}

		@Override
		public void onModelChanged(FriendsModel model) {
			if (isVisible()) {
				syncWithModel();
			}
		}

		@Override
		public void onModelChanged(FriendSuggestionsModel model) {
			if (isVisible()) {
				syncWithModel();
			}
		}

		public void abortSearch() {
			mGlobalSearchAdapter.abortLoad();
		}

		private void resetUnfilteredItems() {
			synchronized (mAllOffers) {
				mAllOffers.clear();
				mAllOffers.addAll(mSuggestionsModel.getOffers());				
			}

			synchronized (mAllSuggestions) {
				mAllSuggestions.clear();
				mAllSuggestions.addAll(mSuggestionsModel.getSuggestions());				
			}

			synchronized (mAllFriends) {
				mAllFriends.clear();
				mAllFriends.addAll(mFriendsModel.getFriends());
			}
		}

		private void filterItems() {
			if (Strings.isNullOrEmpty(mQuery)) {
				mOffers.clear();
				mOffers.addAll(mAllOffers);

				mSuggestions.clear();
				mSuggestions.addAll(mAllSuggestions);

				mFriends.clear();
				mFriends.addAll(mAllFriends);

				notifyDataSetChanged();
			} else {
				mFilter.filter(mQuery);
			}
		}

		private class FilterByQueryResult {
			public Collection<Profile> offers;
			public Collection<Profile> suggestions;
			public Collection<Profile> friends;

			public int size() {
				return offers.size() + suggestions.size() + friends.size();
			}
		}

		private class FilterByQuery extends Filter {
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				if (results == null || results.values == null) return;

				Log.debug(TAG, "publishing filter results");

				FilterByQueryResult data = (FilterByQueryResult)results.values;

				mSuggestions.clear();
				mSuggestions.addAll(data.suggestions);

				mOffers.clear();
				mOffers.addAll(data.offers);

				mFriends.clear();
				mFriends.addAll(data.friends);

				notifyDataSetChanged();

				Log.debug(TAG, "results published");
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				Log.debug(TAG, "performing filtering with " + constraint + " constraint in background");

				if (constraint == null) return null;

				String query = constraint.toString();

				if (query.length() == 0) return null;

				List<Profile> offers;
				List<Profile> suggestions;
				List<Profile> friends;

				synchronized (mAllOffers) {
					offers = new ArrayList<Profile>(mAllOffers);
				}

				synchronized (mAllSuggestions) {
					suggestions = new ArrayList<Profile>(mAllSuggestions);
				}

				synchronized (mAllFriends) {
					friends = new ArrayList<Profile>(mAllFriends);
				}

				FilterByQueryResult result = new FilterByQueryResult();

				result.offers = UserSearch.filterByQuery(offers, query);
				result.suggestions = UserSearch.filterByQuery(suggestions, query);
				result.friends = UserSearch.filterByQuery(friends, query);

				FilterResults results = new FilterResults();

				results.values = result;
				results.count = result.size();

				Log.debug(TAG, "filtering in background completed");

				return results;
			}

		}
	}

	public class UserSearcher implements CollectionLoader<Profile> {
		private final String mQuery;

		private int mOffset;

		public UserSearcher(String query) {
			mQuery = query;
		}

		@Override
		public Profile[] loadFreshData() throws Exception {
			throw new Exception("Not supported");
		}

		@Override
		public ObjectSubset<Profile> loadMoreData() throws Exception {
			List<Profile> users = mVk.findUsers(mQuery, mOffset, 30);

			mOffset += users.size();

			ObjectSubset<Profile> result = new ObjectSubset<Profile>();

			result.content = users.toArray(new Profile[users.size()]);
			result.hasMore = users.size() > 0;

			return result;
		}
	}

	public class GlobalSearchAdapter extends LoaderAdapter<Profile> {
		private List<Profile> mContent;

		public GlobalSearchAdapter(Context context, ListAdapter wrapped, List<Profile> content, String query) {
			super(context, wrapped, new BackwardPreloader<Profile>(sExecutor, new UserSearcher(query)));

			mContent = content;
		}

		@Override
		protected void publishResults(ObjectSubset<Profile> results) {
			mContent.addAll(Arrays.asList(results.content));

			notifyDataSetChanged();
		}
	}

}
