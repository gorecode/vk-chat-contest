package com.gorecode.vk.activity;

import java.util.ArrayList;
import java.util.List;

import roboguice.inject.InjectView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.activity.dialogs.DialogsFragment;
import com.gorecode.vk.activity.friends.FriendsFragment;
import com.gorecode.vk.activity.search.SearchFragment;
import com.gorecode.vk.data.UnhandledNotifications;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.sync.SessionContext.OnUnhandledNotificationsUpdateListener;
import com.gorecode.vk.view.OnBackPressedHandler;

public class MainActivity extends VkFragmentActivity {
	@InjectView(R.id.messagesCount)
	private TextView messageCount;
	@InjectView(R.id.offersCount)
	private TextView offersCount;

	private final List<TabInfo> tabs = new ArrayList<TabInfo>();
	
	private TabInfo messagesTab;
	private TabInfo friendsTab;
	private TabInfo offersTab;
	private TabInfo settingsTab;

	@Inject
	private SessionContext session;

	private TabInfo currentTab;

	public static void display(Activity parent) {
		Intent intent = new Intent(parent, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		parent.startActivity(intent);
	}

	public int getCurrentTabFragmentId() {
		return currentTab.fragment.getId();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_RIGHT);

		setContentView(R.layout.main_activity);

		Aibolit.doInjections(this);

		tabs.add(messagesTab = new TabInfo(DialogsFragment.class, R.drawable.msg, R.drawable.msg_active, R.id.messagesImage));
		tabs.add(friendsTab = new TabInfo(FriendsFragment.class, R.drawable.conference, R.drawable.conference_active, R.id.friendsImage));
		tabs.add(offersTab = new TabInfo(SearchFragment.class, R.drawable.search, R.drawable.search_active, R.id.offersImage));
		tabs.add(settingsTab = new TabInfo(SettingsFragment.class, R.drawable.stg, R.drawable.stg_active, R.id.settingsImage));

		setCurrentTab(messagesTab);
	}	

	@Override
	protected void onResume() {
		super.onResume();

		session.addOnUnhandledNotificationsUpdateListener(onUnhandledNotificationsUpdateListener);

		updateNotificationCount();
	}
	
	@Override
	protected void onPause() {
		session.removeOnUnhandledNotificationsUpdateListener(onUnhandledNotificationsUpdateListener);

		super.onPause();
	}
	
	@Override
	public void onBackPressed() {
		if (currentTab != null && currentTab.fragment instanceof OnBackPressedHandler) {
			OnBackPressedHandler backPressedHandler = (OnBackPressedHandler)currentTab.fragment;

			if (!backPressedHandler.onBackPressed()) {
				finish();
			}
		} else {
			finish();
		}
	}

	@InjectOnClickListener(R.id.messagesButton)
	protected void onMessagesButtonClick(View view) {
		setCurrentTab(messagesTab);
	}

	@InjectOnClickListener(R.id.friendsButton)
	protected void onFriendsButtonClick(View view) {
		setCurrentTab(friendsTab);
	}

	@InjectOnClickListener(R.id.offersButton)
	protected void onPeopleSearchClick(View view) {
		setCurrentTab(offersTab);
	}

	@InjectOnClickListener(R.id.settingsButton)
	protected void onSettingsClick(View view) {
		setCurrentTab(settingsTab);
	}

	private void setCurrentTab(TabInfo tab) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		for (TabInfo eachTab : tabs) {
			if (tab == eachTab) {
				eachTab.captionView.setCompoundDrawablesWithIntrinsicBounds(0, eachTab.imageForActiveState, 0, 0);

				((View)eachTab.captionView.getParent()).setSelected(true);

				if (eachTab.fragment == null) {
					eachTab.fragment = Fragment.instantiate(this, eachTab.clazz.getName(), null);

					transaction.add(R.id.fragmentHolder, eachTab.fragment, eachTab.getFragmentTag());
				} else {
					transaction.show(eachTab.fragment);
				}
			} else {
				eachTab.captionView.setCompoundDrawablesWithIntrinsicBounds(0, eachTab.image, 0, 0);

				((View)eachTab.captionView.getParent()).setSelected(false);

				if (eachTab.fragment != null) {
					transaction.hide(eachTab.fragment);
				}
			}
		}

		currentTab = tab;

		transaction.commit();

		getSupportFragmentManager().executePendingTransactions();
	}
	
	private void updateNotificationCount() {
		if (session.getUser() == null) return;

		UnhandledNotifications summary = session.getUser().notificationSummary;
		
		setNotificationCount(messageCount, summary.numMessages);
		setNotificationCount(offersCount, summary.numOffers);
	}
	
	private void setNotificationCount(TextView view, int notificationCount) {
		if (notificationCount > 0) {
			view.setVisibility(View.VISIBLE);
			view.setText(Integer.toString(notificationCount));
		} else {
			view.setVisibility(View.INVISIBLE);
		}
	}
	
	private final OnUnhandledNotificationsUpdateListener onUnhandledNotificationsUpdateListener = new OnUnhandledNotificationsUpdateListener() {
		@Override
		public void onUnhandledNotificationsUpdate() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (isDestroyed()) return;

					updateNotificationCount();
				}
			});
		}
	};

	private class TabInfo {
		public final int image;
		public final int imageForActiveState;
		public final TextView captionView;
		public Fragment fragment;
		public final Class<? extends Fragment> clazz;

		public TabInfo(Class<? extends Fragment> clazz, int imgResId, int imgResIdForSelectedState, int captionViewId) {
			this.clazz = clazz;
			this.image = imgResId;
			this.imageForActiveState = imgResIdForSelectedState;
			this.captionView = (TextView)findViewById(captionViewId);
			this.fragment = getSupportFragmentManager().findFragmentByTag(getFragmentTag());
		}

		private String getFragmentTag() {
			return clazz.getName();
		}
	}
}
