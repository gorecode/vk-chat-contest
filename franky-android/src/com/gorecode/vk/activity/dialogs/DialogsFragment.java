package com.gorecode.vk.activity.dialogs;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import roboguice.inject.InjectView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.VisibleInjectionContext;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.gorecode.vk.R;
import com.gorecode.vk.activity.GroupChatActivity;
import com.gorecode.vk.activity.PickFriendActivity;
import com.gorecode.vk.activity.VkActivityContract;
import com.gorecode.vk.activity.VkFragment;
import com.gorecode.vk.activity.chat.ChatActivity;
import com.gorecode.vk.activity.chat.ChatMessageActivity;
import com.gorecode.vk.activity.friends.FriendsModel;
import com.gorecode.vk.adapter.Adapters;
import com.gorecode.vk.adapter.EmptyViewVisibilityController;
import com.gorecode.vk.adapter.LoaderAdapter;
import com.gorecode.vk.adapter.MergeAdapter;
import com.gorecode.vk.adapter.SingleViewAdapter;
import com.gorecode.vk.adapter.TableAdapter;
import com.gorecode.vk.adapter.TableLoaderAdapter;
import com.gorecode.vk.adapter.UserListAdapter;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.cache.ChatCache;
import com.gorecode.vk.cache.DialogsCache;
import com.gorecode.vk.collections.SortedTable;
import com.gorecode.vk.collections.Table;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.GroupChatDescriptor;
import com.gorecode.vk.data.ObjectSubset;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.Range;
import com.gorecode.vk.data.loaders.BackwardPreloader;
import com.gorecode.vk.data.loaders.CollectionLoader;
import com.gorecode.vk.data.loaders.CollectionLoaderByOffset;
import com.gorecode.vk.event.AvailabilityChangedEvent;
import com.gorecode.vk.event.ChatMessageStateChangedEvent;
import com.gorecode.vk.event.DialogChangedEvent;
import com.gorecode.vk.event.DialogDeletedEvent;
import com.gorecode.vk.event.LongPollConnectionRestoredEvent;
import com.gorecode.vk.imageloader.GroupChatImageLoader;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.imageloader.ListImageLoader;
import com.gorecode.vk.service.NotificationService;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.task.DeleteDialogTask;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.utilities.MoreFunctions;
import com.gorecode.vk.view.VkActionBar;
import com.gorecode.vk.view.Animations;
import com.gorecode.vk.view.LoaderLayout;
import com.gorecode.vk.view.OnBackPressedHandler;
import com.gorecode.vk.view.SearchView;
import com.gorecode.vk.view.ViewAnimationHelper;
import com.gorecode.vk.view.ViewUtilities;
import com.uva.lang.ThreadFactories;
import com.uva.log.Log;

public class DialogsFragment extends VkFragment implements OnItemClickListener, OnBackPressedHandler {
	public static final String EXTRA_PERSON = VkActivityContract.EXTRA_PERSON;

	private static final String TAG = DialogsFragment.class.getSimpleName();

	private static final Comparator<Dialog> BY_DATE_ASCENDING = new Comparator<Dialog>() {
		@Override
		public int compare(Dialog n1, Dialog n2) {
			return Long.signum(n1.lastMessage.timestamp - n2.lastMessage.timestamp);
		}		
	};

	private static final int ACTIVE_LAYOUT_DIALOGS = 0x0;
	private static final int ACTIVE_LAYOUT_SEARCH = 0x1;

	private static final int REQUEST_CODE_PICK_FRIEND = 0x0;

	private static final ExecutorService sRequestExecutor = new ThreadPoolExecutor(0, 2, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), ThreadFactories.WITH_LOWEST_PRIORITY);

	@Inject
	private VkModel model;
	@Inject
	private EventBus eventBus;
	@Inject
	private DialogsCache dialogsCache;
	@Inject
	private ListImageLoader listImageLoader;
	@Inject
	private FriendsModel friendsModel;
	@Inject
	private SessionContext sessionContext;
	@Inject
	private ImageLoader imageLoader;
	@Inject
	private ChatCache chatCache;

	private SearchView quickSearchView;

	@InjectView(android.R.id.list)
	private ListView listView;
	@InjectView(R.id.listLayout)
	private LoaderLayout listLayout;
	@InjectView(R.id.dialogsLayout)
	private ViewGroup dialogsLayout;
	@InjectView(R.id.messagesLayout)
	private ViewGroup searchLayout;
	@InjectView(R.id.popup_menu)
	private View popupMenu;
	private ViewAnimationHelper popupMenuAnimator;

	private VkActionBar actionBar;

	private DialogsData dialogs;
	private DialogsListModel dialogListItems;
	private BackwardPreloader<DialogListItem> dialogsLoader;
	private DialogsDataAdapter dialogsAdapter;
	private DialogsLoaderAdapter dialogsLoaderAdapter;

	private SearchAdapter searchAdapter;

	private int activeLayout = ACTIVE_LAYOUT_DIALOGS;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);

		VkApplication.getApplication().getInjector().injectMembers(this);

		dialogs = new DialogsData();
		dialogListItems = new DialogsListModel(dialogs);
		dialogs.addAll(dialogsCache.findAll());
		dialogs.notifyTableChanged();

		dialogsLoader = new BackwardPreloader<DialogListItem>(sRequestExecutor, new DialogsLoader(dialogListItems));
		dialogsLoader.preloadMoreData();
		dialogsAdapter = new DialogsDataAdapter(dialogListItems);
		dialogsLoaderAdapter = new DialogsLoaderAdapter();

		eventBus.register(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.dialogs_fragment, container, false);

		return root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(null);

		View retryButton = listLayout.getErrorView().findViewById(R.id.retry);

		if (retryButton != null) {
			retryButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					syncDialogs();
				}
			});
		}

		popupMenu.setVisibility(View.GONE);
		popupMenuAnimator = new ViewAnimationHelper(popupMenu, Animations.POPUP_HIDE_ANIMATION, Animations.POPUP_SHOW_ANIMATION);

		actionBar = (VkActionBar)getView().findViewById(R.id.actionBar);
		actionBar.setActionButtonOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onActionBarButtonClicked();
			}
		});

		listImageLoader.registerFetcher(new GroupChatImageLoader(getActivity(), imageLoader));

		quickSearchView = new SearchView(getActivity());
		quickSearchView.setOnQueryTextListener(onQuickSearchQueryEventHandler);

		registerForContextMenu(listView);

		dialogsLoaderAdapter.setMixinDisplayment(TableLoaderAdapter.DISPLAYMENT_NO_MIXIN);
		dialogsLoaderAdapter.displayLoadStateInLoaderLayoutOnLoadingFromScratch(listLayout);

		EmptyViewVisibilityController.bind(dialogsLoaderAdapter, dialogsLayout.findViewById(android.R.id.empty));

		listView.setOnScrollListener(listImageLoader);
		listView.addHeaderView(quickSearchView);
		listView.setOnItemClickListener(this);
		listView.setDrawingCacheEnabled(false);

		syncDialogs();

		setActiveLayout(ACTIVE_LAYOUT_DIALOGS);

		Aibolit.doInjections(this, new VisibleInjectionContext(getView()));
	}

	@Override
	public void onResume() {
		super.onResume();

		NotificationService notificationService = NotificationService.getSharedInstance();

		if (notificationService != null) {
			notificationService.removeNotification();
		}
	}

	@Override
	public void onDestroy() {
		eventBus.unregister(this);

		listImageLoader.stop();

		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_PICK_FRIEND && resultCode == Activity.RESULT_OK) {
			Profile friend = PickFriendActivity.getActivityResult(data);

			startActivity(ChatActivity.getDisplayIntent(getActivity(), friend));
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater menuInflater = new MenuInflater(getActivity());

		if (v == listView) {
			if (activeLayout == ACTIVE_LAYOUT_DIALOGS) {
				menuInflater.inflate(R.menu.item_dialog, menu);
			} else {
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

				Object item = listView.getAdapter().getItem(info.position);

				if (item != null && item instanceof DialogListItem) {
					menuInflater.inflate(R.menu.item_message, menu);
				}
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete: {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

			Dialog dialog = dialogs.getById(info.id);

			if (dialog != null) {
				onDeleteDialogClicked(dialog);
			}

			return true;
			}
		case R.id.chat:
		case R.id.view: {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

			Object itemAtPosition = listView.getItemAtPosition(info.position);

			if (itemAtPosition != null && itemAtPosition instanceof DialogListItem) {
				Dialog dialog = ((DialogListItem)itemAtPosition).dialog;

				if (item.getItemId() == R.id.chat) {
					startActivity(ChatActivity.getDisplayIntent(getActivity(), dialog));
				}
				if (item.getItemId() == R.id.view) {
					startActivity(ChatMessageActivity.getDisplayIntent(getActivity(), dialog.lastMessage));
				}
			}

			return true;
			}
		}

		return super.onContextItemSelected(item);
	}


	@Override
	public boolean onBackPressed() {
		if (popupMenu.getVisibility() == View.VISIBLE) {
			popupMenuAnimator.hideView();

			return true;
		} else {
			return false;
		}
	}

	@InjectOnClickListener(R.id.popup_item_compose_message)
	public void onComposeMessagePopupItemClicked(View v) {
		popupMenuAnimator.hideView();

		startActivityForResult(new Intent(getActivity(), PickFriendActivity.class), REQUEST_CODE_PICK_FRIEND);
	}

	@InjectOnClickListener(R.id.popup_item_create_group_chat) 
	public void onCreateGroupChatPopupItemClicked(View v) {
		popupMenuAnimator.hideView();

		startActivity(new Intent(getActivity(), GroupChatActivity.class));
	}

	@Subscribe
	public void onLongPollConnectionRestored(LongPollConnectionRestoredEvent event) {
		syncDialogs();
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Object item = adapterView.getItemAtPosition(position);

		if (item instanceof DialogListItem) {
			Dialog dialog = ((DialogListItem)item).dialog;

			Intent i = ChatActivity.getDisplayIntent(getActivity(), dialog);

			if (activeLayout == ACTIVE_LAYOUT_SEARCH) {
				i = ChatMessageActivity.getDisplayIntent(getActivity(), dialog.lastMessage);
			}

			startActivity(i);
		} else if (item instanceof Profile) {
			Intent i = ChatActivity.getDisplayIntent(getActivity(), (Profile)item);

			startActivity(i);
		} else if (item instanceof GroupChatDescriptor) {
			Intent i = ChatActivity.getDisplayIntent(getActivity(), (GroupChatDescriptor)item);

			startActivity(i);
		}
	}

	@Subscribe
	public void onMessageStateChanged(ChatMessageStateChangedEvent event) {
		for (Dialog dialog : dialogs) {
			if (dialog.lastMessage.id == event.messageId) {
				event.apply(dialog.lastMessage);
				replaceLastMessage(dialog.lastMessage);
				break;
			}
		}
		dialogs.notifyTableChanged();
	}

	@Subscribe
	public void onDialogDeleted(DialogDeletedEvent event) {
		dialogs.removeById(getDialogId(event.dialog));
		dialogs.notifyTableChanged();
	}

	@Subscribe
	public void onDialogChanged(DialogChangedEvent event) {
		dialogs.put(event.dialog);
		dialogs.notifyTableChanged();
	}

	@Subscribe
	public void onAvailabilityChanged(final AvailabilityChangedEvent event) {
		Dialog dialog = dialogs.getById(event.getUserId());

		if (dialog != null) {
			dialog.getParticipant().availability = event.getAvailability();

			dialogs.put(dialog);
			dialogs.notifyTableChanged();
		}
	}

	@Subscribe
	public void onNewMessage(final ChatMessage message) {
		if (isRemoving() || isDetached()) return;

		Log.trace(TAG, "got new message");

		if (message.isFromConference() && !dialogs.containsKey(getDialogId(message))) {
			Log.debug(TAG, "message from unknown group chat");

			new LongAction<Void, Dialog>(getActivity()) {
				@Override
				protected Dialog doInBackgroundOrThrow(Void params) throws Exception {
					Dialog newDialog = model.getDialogById(message.chatId);

					if (!Thread.currentThread().isInterrupted()) {
						dialogsCache.save(newDialog);
					}

					return newDialog;
				}

				@Override
				public void onSuccess(Dialog newDialog) {
					dialogs.put(newDialog);
					dialogs.notifyTableChanged();
				}
			}.execute();
		} else {
			replaceLastMessage(message);

			dialogs.notifyTableChanged();
		}
	}

	private void setLastUpdateTime(long timeMillis) {
		dialogsCache.setLastUpdateTime(timeMillis);
	}

	private void onActionBarButtonClicked() {
		if (popupMenu.getVisibility() == View.VISIBLE) {
			popupMenuAnimator.hideView();
		} else {
			popupMenuAnimator.showView();
		}
	}

	private void onDeleteDialogClicked(Dialog dialog) {
		new DeleteDialogTask(getActivity(), dialog).execute();
	}

	private LongAction<?, ?> pendingSync;

	private void syncDialogs() {
		if (pendingSync != null) {
			pendingSync.abort();
			pendingSync = null;
		}

		final boolean isFirstSync = dialogs.size() == 0;

		pendingSync = new LongAction<Void, ObjectSubset<DialogListItem>>(getActivity()) {
			@Override
			public void blockUi() {
				if (dialogs.size() == 0) {
					listLayout.displayLoadView();
				}
			}

			@Override
			public void unblockUi() {
				if (dialogs.size() == 0) {
					listLayout.displayContent();
				}
			}

			@Override
			protected ObjectSubset<DialogListItem> doInBackgroundOrThrow(Void params) throws Exception {
				ObjectSubset<DialogListItem> result = null;

				if (dialogs.size() > 0) {
					result = ObjectSubset.from(dialogsLoader.loadFreshData());
				} else {
					result = dialogsLoader.loadMoreData();
				}

				if (!Thread.currentThread().isInterrupted()) {
					dialogsCache.deleteAll();

					saveToCache(Collections2.transform(Arrays.asList(result.content), DialogListItem.FUNCTION_TO_DIALOG));
				}

				return result;
			}

			@Override
			protected void onError(Exception e) {
				if (isFirstSync) {
					friendsModel.sync(VkApplication.from(getContext()));
				}

				pendingSync = null;

				if (dialogs.size() == 0) {
					listLayout.displayErrorView();
				}

				Log.exception(TAG, "Error refreshing dialogs", e);
			}

			@Override
			protected void onSuccess(ObjectSubset<DialogListItem> newDialogs) {
				if (isFirstSync) {
					friendsModel.sync(VkApplication.from(getContext()));
				}

				pendingSync = null;

				if (newDialogs.hasMore) {
					dialogsLoaderAdapter.setMixinDisplayment(TableLoaderAdapter.DISPLAYMENT_LOADING);
				}

				dialogs.clear();
				dialogs.putAll(Iterables.transform(Arrays.asList(newDialogs.content), DialogListItem.FUNCTION_TO_DIALOG));
				dialogs.notifyTableChanged();

				setLastUpdateTime(System.currentTimeMillis());
			}
		};
		pendingSync.execute();
	}

	private void replaceLastMessage(ChatMessage message) {
		Dialog dialog = dialogs.getById(getDialogId(message));

		if (dialog != null) {
			dialogs.removeById(getDialogId(dialog));
		}

		if (dialog == null) {
			dialog = Dialog.withOneMessage(message);
		} else {
			dialog = dialog.clone();
			dialog.lastMessage = message;
		}

		dialogs.put(dialog);
	}

	private void saveToCache(Iterable<Dialog> items) {
		dialogsCache.save(items);
		chatCache.save(Iterables.transform(items, MoreFunctions.getLastMessage()));
	}

	private void abortSearch() {
		if (searchAdapter != null) {
			searchAdapter.abortSearch();
		}
		searchAdapter = null;
	}

	private void performQuickSearch(String query) {
		abortSearch();

		searchAdapter = new SearchAdapter(query);
		searchAdapter.performSearch();

		setActiveLayout(ACTIVE_LAYOUT_SEARCH);
	}

	private ListAdapter currentListAdapter;

	private void setListAdapter(ListAdapter adapter) {
		if (currentListAdapter == adapter) {
			return;
		}

		listView.setAdapter(adapter);

		currentListAdapter = adapter;
	}

	private void setActiveLayout(int activeLayout) {
		this.activeLayout = activeLayout;

		updateViews();		
	}

	private void updateViews() {
		if (activeLayout == ACTIVE_LAYOUT_DIALOGS) {
			dialogsLayout.setVisibility(View.VISIBLE);

			searchLayout.setVisibility(View.GONE);

			setListAdapter(dialogsLoaderAdapter);
		}
		if (activeLayout == ACTIVE_LAYOUT_SEARCH) {
			searchLayout.setVisibility(View.VISIBLE);

			dialogsLayout.setVisibility(View.GONE);

			setListAdapter(searchAdapter);
		}
	}

	private static long getDialogId(ChatMessage message) {
		if (message.isFromConference()) {
			return message.chatId;
		} else {
			return message.getParticipant().id;
		}
	}

	private static long getDialogId(Dialog dialog) {
		return getDialogId(dialog.lastMessage);
	}

	private final SearchView.OnQueryTextListenerAdapter onQuickSearchQueryEventHandler = new SearchView.OnQueryTextListenerAdapter() {
		@Override
		public boolean onQueryTextSubmit(String query) {
			query = query.trim();

			if (query.length() > 0) {
				ViewUtilities.hideSoftInput(quickSearchView.getQueryEdit());
			}

			return true;
		}

		@Override
		public boolean onQueryTextChange(String query) {
			query = query.trim();

			if (query.length() == 0) {
				abortSearch();

				setActiveLayout(ACTIVE_LAYOUT_DIALOGS);
			} else {
				performQuickSearch(query);
			}

			return true;
		}
	};

	private class MessagesLoader implements CollectionLoader<DialogListItem> {
		private volatile int mOffset;

		private final String mQuery;

		public MessagesLoader(String query) {
			mQuery = query;
		}

		@Override
		public DialogListItem[] loadFreshData() throws Exception {
			throw new Exception("Not supported");
		}

		@Override
		public ObjectSubset<DialogListItem> loadMoreData() throws Exception {
			ObjectSubset<DialogListItem> result = new ObjectSubset<DialogListItem>();

			List<ChatMessage> messages = model.getMessagesByQuery(mQuery, mOffset, 15);

			DialogListItem[] dialogs = new DialogListItem[messages.size()];

			for (int i = 0; i < messages.size(); i++) {
				dialogs[i] = DialogListItem.forDialog(Dialog.withOneMessage(messages.get(i)));
			}

			result.content = dialogs;
			result.hasMore = result.content.length > 0;

			mOffset += result.content.length;

			return result;
		}
	}

	private class MessagesAdapter extends com.gorecode.vk.adapter.ListAdapter<DialogListItem> {
		public MessagesAdapter(List<DialogListItem> data) {
			super(data);
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			final boolean isIncoming = getItem(position).dialog.lastMessage.isIncoming();

			return isIncoming ? 0 : 1;
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).dialog.lastMessage.id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DialogListItem item = getItem(position);

			long startTime = System.currentTimeMillis();

			if (convertView == null) { 
				convertView = new DialogListItemView(getActivity(), sessionContext.getUser(), listImageLoader, item.dialog);
			}

			((DialogListItemView)convertView).setItem(item);

			Log.debug(TAG, "getView() took " + (System.currentTimeMillis() - startTime) + "ms");

			return convertView;
		}
	}

	private class MessagesLoaderAdapter extends LoaderAdapter<DialogListItem> {
		public MessagesLoaderAdapter(Context context, MessagesAdapter wrapped, CollectionLoader<DialogListItem> loader) {
			super(context, wrapped, loader);
		}

		@Override
		protected void publishResults(ObjectSubset<DialogListItem> data) {
			MessagesAdapter adapter = (MessagesAdapter)getWrappedAdapter();
			adapter.addAll(Arrays.asList(data.content));
			adapter.notifyDataSetChanged();
		}
	}

	private class DialogsData extends SortedTable<Dialog> {
		public DialogsData() {
			super(Collections.reverseOrder(BY_DATE_ASCENDING));
		}

		@Override
		public long getIdOfObject(Dialog object) {
			return getDialogId(object);
		}
	};

	public class SearchAdapter extends MergeAdapter {
		private volatile boolean mIsShutdown;

		private final String mQuery;

		private LongAction<?, ?> mPendingSearch;
		private LoaderAdapter<?> mMessagesSearchAdapter;

		private ListAdapter mLoadViewAdapter;

		@Override
		public int getViewTypeCount() {
			return 10;
		}

		public SearchAdapter(String query) {
			mQuery = query;
		}

		private void addLoadItem() {
			if (mLoadViewAdapter != null) {
				return;
			}

			mLoadViewAdapter = new SingleViewAdapter(getActivity(), R.layout.item_loading, ListView.ITEM_VIEW_TYPE_IGNORE);

			addAdapter(mLoadViewAdapter);
		}

		private void removeLoadItem() {
			if (mLoadViewAdapter == null) {
				return;
			}

			removeAdapter(mLoadViewAdapter);

			mLoadViewAdapter = null;
		}

		public void abortSearch() {
			if (mPendingSearch != null) {
				mPendingSearch.abort();
				mPendingSearch = null;
			}
			if (mMessagesSearchAdapter != null) {
				mMessagesSearchAdapter.abortLoad();
				mMessagesSearchAdapter = null;
			}
		}

		public void performSearch() {
			if (mPendingSearch != null) {
				return;
			}

			searchLayout.findViewById(android.R.id.empty).setVisibility(View.GONE);

			addLoadItem();

			mPendingSearch = new LongAction<Void, Void>(getActivity()) {
				@Override
				protected Void doInBackgroundOrThrow(Void params) throws Exception {
					performSearchSync();

					return null;
				}
			};
			mPendingSearch.execute();
		}

		private void performSearchSync() {
			try {
				final List<Profile> friends = friendsModel.getFriendsFilteredByQuery(mQuery);

				if (mIsShutdown) return;

				if (friends.size() > 0) {
					runOnUiThreadIfNotCancelled(new Runnable() {
						@Override
						public void run() {
							removeLoadItem();

							ListAdapter friendsAdapter = new UserListAdapter(getActivity(), listImageLoader, friends);

							addAdapter(Adapters.newSectionAdapter(getActivity(), friendsAdapter, R.string.list_item_separator_friends));

							addLoadItem();

							notifyDataSetChanged();
						}
					});
				}

				Log.debug(TAG, "Searching group chats with query = " + mQuery);

				final List<Pair<Profile, GroupChatDescriptor>> searchResult = model.searchDialogs(mQuery);

				if (mIsShutdown) return;

				final List<Profile> people = new ArrayList<Profile>();
				final List<GroupChatDescriptor> groupChats = new ArrayList<GroupChatDescriptor>();

				for (Pair<Profile, GroupChatDescriptor> item : searchResult) {
					if (item.first != null) {
						people.add(item.first);
					}
					if (item.second != null) {
						groupChats.add(item.second);
					}
				}

				for (Profile friend : friends) {
					for (int i = 0; i < people.size(); i++) {
						Profile user = people.get(i);
						if (friend.id == user.id) {
							people.remove(i);
							break;
						}
					}
				}

				if (people.size() > 0) {
					runOnUiThreadIfNotCancelled(new Runnable() {
						@Override
						public void run() {
							removeLoadItem();

							ListAdapter contentAdapter = new UserListAdapter(getActivity(), listImageLoader, people);

							addAdapter(Adapters.newSectionAdapter(getActivity(), contentAdapter, R.string.list_item_separator_people));

							addLoadItem();

							notifyDataSetChanged();
						}
					});
				}

				if (groupChats.size() > 0) {
					runOnUiThreadIfNotCancelled(new Runnable() {
						@Override
						public void run() {
							removeLoadItem();

							ListAdapter contentAdapter = new GroupChatDescriptorAdapter(groupChats);

							addAdapter(Adapters.newSectionAdapter(getActivity(), contentAdapter, R.string.list_item_separator_dialogs));

							addLoadItem();

							notifyDataSetChanged();
						}
					});
				}

				if (mIsShutdown) return;

				Log.debug(TAG, "searching for messages");

				final List<DialogListItem> messages = new ArrayList<DialogListItem>();

				final MessagesLoader messagesLoader = new MessagesLoader(mQuery);

				final CollectionLoader<DialogListItem> messagesPreloader = new BackwardPreloader<DialogListItem>(sRequestExecutor, messagesLoader);

				messages.addAll(Arrays.asList(messagesPreloader.loadMoreData().content));

				runOnUiThreadIfNotCancelled(new Runnable() {
					@Override
					public void run() {
						removeLoadItem();

						if (messages.size() > 0) {
							final MessagesAdapter contentAdapter = new MessagesAdapter(messages);

							final MessagesLoaderAdapter contentLoaderAdapter = new MessagesLoaderAdapter(getActivity(), contentAdapter, messagesPreloader);

							mMessagesSearchAdapter = contentLoaderAdapter;

							contentLoaderAdapter.loadMoreData();
							contentLoaderAdapter.setLoadMoreDataOnPendingViewInflate(true);

							addAdapter(Adapters.newSectionAdapter(getActivity(), contentLoaderAdapter, R.string.list_item_separator_messages));
						}

						notifyDataSetChanged();

						EmptyViewVisibilityController.bind(SearchAdapter.this, searchLayout.findViewById(android.R.id.empty));
					}
				});
			} catch (InterruptedException e) {
				;
			} catch (InterruptedIOException e) {
				;
			} catch (Exception e) {
				Log.exception(TAG, "error searching dialogs and messages", e);
			} 
		}

		private void runOnUiThreadIfNotCancelled(final Runnable action) {
			Activity activity = getActivity();

			if (activity != null) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mIsShutdown) return;

						action.run();
					}
				});
			}
		}
	}

	private class GroupChatDescriptorAdapter extends com.gorecode.vk.adapter.ListAdapter<GroupChatDescriptor> {
		public GroupChatDescriptorAdapter(List<GroupChatDescriptor> list) {
			super(list);
		}

		@Override
		public long getItemId(int position) {
			GroupChatDescriptor item = getItem(position);

			return GroupChatDescriptor.convertChatIdToUid(item.chatId);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new GroupChatDescriptorView(getActivity(), listImageLoader);
			}

			GroupChatDescriptor item = getItem(position);

			((GroupChatDescriptorView)convertView).setGroupChat(item);

			return convertView;
		}
	}

	private class DialogsLoader extends CollectionLoaderByOffset<DialogListItem> {
		private boolean mHasMore = true;

		public DialogsLoader(Collection<DialogListItem> data) {
			super(data);
		}

		@Override
		public ObjectSubset<DialogListItem> loadMoreData() throws Exception {
			ObjectSubset<DialogListItem> result = super.loadMoreData();

			if (!Thread.currentThread().isInterrupted()) {
				saveToCache(Collections2.transform(Arrays.asList(result.content), DialogListItem.FUNCTION_TO_DIALOG));
			}

			return result;
		}

		@Override
		public ObjectSubset<DialogListItem> loadDataPage(int rowOffset, int rowLimit) throws Exception {
			Range range = new Range(rowOffset, rowLimit);
			Log.trace(TAG, "query message notifications offset=" + range.offset + " count=" + range.limit);
			return loadDataByRange(range);
		}

		private ObjectSubset<DialogListItem> loadDataByRange(Range rowRange) throws Exception {
			List<DialogListItem> dialogs = Lists.transform(model.getDialogs(rowRange.offset, rowRange.limit), DialogListItem.FUNCTION_TO_ITEM);
			
			mHasMore = dialogs.size() > 0;

			return new ObjectSubset<DialogListItem>(dialogs.toArray(new DialogListItem[dialogs.size()]), mHasMore);
		}
	}

	private class DialogsLoaderAdapter extends TableLoaderAdapter<DialogListItem> {
		public DialogsLoaderAdapter() {
			super(getActivity(), dialogsLoader, dialogListItems, dialogsAdapter);
		}
	}

	private class DialogsDataAdapter extends TableAdapter<DialogListItem> {
		public DialogsDataAdapter(Table<DialogListItem> content) {
			super(content);
		}

		@Override
		public long getItemId(int position) {
			return getDialogId(getItem(position).dialog);
		}

		@Override
		public int getItemViewType(int position) {
			final boolean isIncoming = getItem(position).dialog.lastMessage.isIncoming();

			return isIncoming ? 0 : 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DialogListItem item = getItem(position);

			long startTime = System.currentTimeMillis();

			if (convertView == null) { 
				convertView = new DialogListItemView(getActivity(), sessionContext.getUser(), listImageLoader, item.dialog);
			}

			((DialogListItemView)convertView).setItem(item);

			Log.debug(TAG, "getView() took " + (System.currentTimeMillis() - startTime) + "ms");

			return convertView;
		}
	}
}
