package com.gorecode.vk.activity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.common.collect.Collections2;
import com.google.common.collect.ForwardingList;
import com.google.inject.Inject;
import com.gorecode.vk.activity.chat.ChatActivity;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.GroupChatDescriptor;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.imageloader.ListImageLoader;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.utilities.MoreFunctions;
import com.gorecode.vk.utilities.Toasts;
import com.gorecode.vk.view.LoaderLayout;
import com.gorecode.vk.view.UserListItemView;
import com.uva.log.Log;

@ContentView(R.layout.group_chat_activity)
public class GroupChatActivity extends VkActivity implements OnItemClickListener {
	private static final String TAG = GroupChatActivity.class.getSimpleName();

	private static final String EXTRA_GROUP_CHAT_ID = "chatId";

	private static final int MODE_EDIT = 0x0;
	private static final int MODE_CREATE = 0x1;

	private static final int REQUEST_CODE_PICK_FRIEND = 0x0;

	@InjectView(android.R.id.list)
	private ListView mListView;
	private View mListViewFooter;

	private EditText mDialogTitleView;
	private Button mChangeDialogTitleButton;

	private Button mCreateDialogButton;

	private ParticipantsAdapter mParticipansAdapter;

	private State mState;

	private int mMode;

	@Inject
	private VkModel mModel;
	@Inject
	private SessionContext mSessionContext;
	@Inject
	private ListImageLoader mListImageLoader;

	public static Intent getDisplayIntent(Context context, long chatId) {
		Intent intent = new Intent(context, GroupChatActivity.class);
		intent.putExtra(EXTRA_GROUP_CHAT_ID, chatId);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_BOTTOM);

		mState = new State();

		init();

		setUpViews();

		if (mMode == MODE_EDIT) {
			loadDataAsync();
		} else {
			updateViews();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_PICK_FRIEND && resultCode == RESULT_OK) {
			final Profile friend = PickFriendActivity.getActivityResult(data);

			if (mState.descriptor.findUser(friend.id) != null) {
				return;
			}

			if (mMode == MODE_EDIT) {
				LongAction<?, ?> action = new LongAction<Void, Void>(this) {
					@Override
					protected Void doInBackgroundOrThrow(Void params) throws Exception {
						mModel.addParticipantToDialog(mState.descriptor.getChatId(), friend.id);

						return null;
					}

					@Override
					protected void onError(Exception error) {
						Log.exception(TAG, "Error adding participant", error);

						ErrorHandlingUtilities.displayErrorSoftly(GroupChatActivity.this, error);

						mState.descriptor.removeUser(friend.id);

						notifyDataChanged();
					}

					@Override
					protected void onSuccess(Void unused) {
						; // TODO: Fire dialog changed event.
					}
				};

				action.execute();
			} else {
				mState.descriptor.addUser(friend);
			}

			mState.descriptor.addUser(friend);

			notifyDataChanged();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Profile user = mParticipansAdapter.getItem(position);

		ChatActivity.display(this, user);
	}

	@InjectOnClickListener(R.id.retry)
	protected void onRetryButtonClick(View view) {
		loadDataAsync();
	}

	@InjectOnClickListener(R.id.add_participant_button)
	protected void onAddParticipantClicked(View view) {
		startActivityForResult(new Intent(this, PickFriendActivity.class), REQUEST_CODE_PICK_FRIEND);
	}

	@InjectOnClickListener(R.id.change_dialog_title_button)
	protected void onChangeTitleClicked(View view) {
		final String newTitle = mDialogTitleView.getText().toString().trim();

		if (mMode == MODE_EDIT) {
			LongAction<String, Void> action = new LongAction<String, Void>(this) {
				@Override
				public void blockUi() {
					mChangeDialogTitleButton.setEnabled(false);
				}

				@Override
				public void unblockUi() {
					mChangeDialogTitleButton.setEnabled(true);
				}

				@Override
				protected Void doInBackgroundOrThrow(String newTitle) throws Exception {
					try {
						mModel.changeDialogTitle(mState.descriptor.getChatId(), newTitle);
					} catch (Exception e) {
						Log.exception(TAG, "Error changing dialog title", e);

						throw e;
					}

					return null;
				}

				@Override
				protected void onSuccess(Void unused) {
					; // TODO: Fire dialog changed event.

					mState.descriptor.setTitle(newTitle);
				}
			};

			action.execute(newTitle);
		} else {
			mState.descriptor.setTitle(newTitle);
		}
	}

	@InjectOnClickListener(R.id.create_group_chat_button)
	protected void onCreateGroupChatButtonClicked(View view) {
		final GroupChatDescriptor chat = mState.descriptor;

		chat.setTitle(mDialogTitleView.getText().toString());

		if (chat.users.size() == 0) {
			Toasts.makeText(this, R.string.toast_participants_required).show();

			return;
		}

		LongAction<?, ?> action = new LongAction<Void, Long>(this) {
			@Override
			protected Long doInBackgroundOrThrow(Void params) throws Exception {
				Collection<Long> uids = Collections2.transform(chat.users, MoreFunctions.getUid());

				return mModel.createGroupChat(uids, chat.title);
			}

			@Override
			protected void onSuccess(Long chatId) {
				mMode = MODE_EDIT;

				chat.chatId = chatId;

				updateViews();

				finish();

				Context context = GroupChatActivity.this;

				context.startActivity(ChatActivity.getDisplayIntent(context, chat));
			}
		};
		action.wrapWithProgress(false);
		action.execute();
	}

	protected void onRemovePatricipantClicked(final Profile participant) {
		if (mMode == MODE_EDIT) {
			LongAction<Void, Void> action = new LongAction<Void, Void>(this) {
				@Override
				public void blockUi() {
					mState.pendingRemovals.put(participant.id, participant);

					notifyDataChanged();
				}

				@Override
				public void unblockUi() {
					mState.pendingRemovals.remove(participant.id);

					notifyDataChanged();
				}

				@Override
				protected Void doInBackgroundOrThrow(Void unused) throws Exception {
					mModel.removeParticipantFromDialog(mState.descriptor.getChatId(), participant.id);

					return null;
				}

				@Override
				protected void onSuccess(Void unused) {
					mState.descriptor.removeUser(participant.id);

					notifyDataChanged();
				}
			};

			action.execute();
		} else {
			mState.descriptor.removeUser(participant.id);

			notifyDataChanged();
		}
	}

	private void loadDataAsync() {
		LongAction<Void, GroupChatDescriptor> action = new LongAction<Void, GroupChatDescriptor>(this) {
			@Override
			protected GroupChatDescriptor doInBackgroundOrThrow(Void params) throws Exception {
				return mModel.getGroupChatDescriptor(mState.descriptor.getChatId());
			}

			@Override
			protected void onSuccess(GroupChatDescriptor descriptor) {
				mState.descriptor = descriptor;

				notifyDataChanged();
			}
		};
		action.wrapWithSpinner((LoaderLayout)findViewById(R.id.loaderLayout));
		action.execute();
	}

	private boolean isIAmAdmin() {
		return mState.descriptor.ownerId == mSessionContext.getUserId();
	}

	private void notifyDataChanged() {
		mParticipansAdapter.notifyDataSetChanged();

		updateViews();
	}

	private void updateViews() {
		String fmt = getString(R.string.edit_dialog_action_bar_title_format);

		getVkActionBar().setTitle(String.format(fmt, mState.descriptor.users.size()));

		if (mMode == MODE_EDIT) {
			mDialogTitleView.setText(mState.descriptor.getTitle());
		}

		mListViewFooter.setVisibility(isIAmAdmin() ? View.VISIBLE : View.GONE);

		mChangeDialogTitleButton.setVisibility(mMode == MODE_EDIT ? View.VISIBLE : View.GONE);

		mCreateDialogButton.setVisibility(mMode == MODE_CREATE ? View.VISIBLE : View.GONE);
		mCreateDialogButton.setEnabled(mState.descriptor.users.size() > 0);
	}

	private void setUpViews() {
		mParticipansAdapter = new ParticipantsAdapter();

		mListViewFooter = LayoutInflater.from(this).inflate(R.layout.group_chat_activity_footer, null);

		mListView.addFooterView(mListViewFooter);
		mListView.setAdapter(mParticipansAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnScrollListener(mListImageLoader);

		mDialogTitleView = (EditText)findViewById(R.id.dialog_title);

		mCreateDialogButton = (Button)findViewById(R.id.create_group_chat_button);
		mChangeDialogTitleButton = (Button)findViewById(R.id.change_dialog_title_button);

		Aibolit.doInjections(this);

		updateViews();
	}

	private boolean init() {
		mState = new State();
		mState.descriptor = new GroupChatDescriptor();

		Intent intent = getIntent();

		if (intent.hasExtra(EXTRA_GROUP_CHAT_ID)) {
			mState.descriptor.setChatId(intent.getLongExtra(EXTRA_GROUP_CHAT_ID, -1));

			mMode = MODE_EDIT;

			return true;
		} else {
			mState.descriptor.ownerId = mSessionContext.getUserId();

			mMode = MODE_CREATE;

			return false;
		}
	}

	private class ParticipantsAdapter extends com.gorecode.vk.adapter.ListAdapter<Profile> {
		public ParticipantsAdapter() {
			super(new ForwardingList<Profile>() {
				@Override
				protected List<Profile> delegate() {
					return mState.descriptor.users;
				}
			});
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new ParticipantListItemView();
			}

			ParticipantListItemView view = (ParticipantListItemView)convertView;

			view.setUser(getItem(position));

			return view;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).id;
		}
	}

	private class ParticipantListItemView extends UserListItemView {
		private View mRemoveButton;
		private View mRemovePendingView;

		public ParticipantListItemView() {
			super(GroupChatActivity.this, R.layout.item_participant);

			setUpViews();

			setImageLoader(mListImageLoader);
		}

		@Override
		public void setUser(Profile user) {
			super.setUser(user);

			updateViews();
		}

		private void updateViews() {
			if (isIAmAdmin()) {
				boolean isRemovalPending = mState.pendingRemovals.containsKey(getUser().id);

				mRemoveButton.setVisibility(isRemovalPending ? GONE : VISIBLE);
				mRemovePendingView.setVisibility(isRemovalPending ? VISIBLE : GONE);
			} else {
				mRemoveButton.setVisibility(GONE);
				mRemovePendingView.setVisibility(GONE);
			}
		}

		private void setUpViews() {
			mRemoveButton = findViewById(R.id.remove_from_dialog_button);
			mRemoveButton.setOnClickListener(mOnRemoveFromDialogButtonClicked);
			mRemovePendingView = findViewById(R.id.spinner);
		}

		private final View.OnClickListener mOnRemoveFromDialogButtonClicked = new OnClickListener() {
			@Override
			public void onClick(View v) {
				onRemovePatricipantClicked(getUser());
			}
		};
	}

	private static class State {
		public GroupChatDescriptor descriptor;

		public final HashMap<Long, Profile> pendingRemovals = new HashMap<Long, Profile>();
	}
}
