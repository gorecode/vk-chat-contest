package com.gorecode.vk.activity.friends;

import java.util.ArrayList;
import java.util.List;

import roboguice.inject.InjectView;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.TextView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.VisibleInjectionContext;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.gorecode.vk.activity.VkFragment;
import com.gorecode.vk.activity.chat.ChatActivity;
import com.gorecode.vk.activity.friends.FriendsPageView.OnFriendItemClickListener;
import com.gorecode.vk.data.Profile;

public class FriendsFragment extends VkFragment implements OnFriendItemClickListener {
	private static final String ARG_HIDE_CONTACTS_TAG = "hideContactsTab";

	@InjectView(R.id.buttonFriends)
	private TextView mFriendsButton;
	@InjectView(R.id.buttonFriendsOnline)
	private TextView mFriendsOnlineButton;
	@InjectView(R.id.buttonContacts)
	private TextView mContactsButton;

	@InjectView(R.id.fragmentsLayout)
	private ViewGroup mPagesLayout;
	@InjectView(R.id.friendsPage)
	private FriendsPageView mFriendsPage;
	@InjectView(R.id.friendsOnlinePage)
	private FriendsPageView mFriendsOnlinePage;

	private ContactsPageView mContactsPage;

	private List<PageDescriptor> mPages = new ArrayList<PageDescriptor>();

	private OnFriendItemClickListener mOnFriendItemClickListener;

	public static Fragment newInstance(Context context, boolean hideContacts) {
		Bundle args = new Bundle();
		args.putBoolean(ARG_HIDE_CONTACTS_TAG, hideContacts);
		return Fragment.instantiate(context, FriendsFragment.class.getName(), args);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.friends_fragment, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mPages.add(new PageDescriptor(mFriendsButton, mFriendsPage));
		mPages.add(new PageDescriptor(mFriendsOnlineButton, mFriendsOnlinePage));

		boolean contactsEnabled = true;

		Bundle args = getArguments();

		if (args != null && args.getBoolean(ARG_HIDE_CONTACTS_TAG, false)) {
			contactsEnabled = false;
		}

		if (contactsEnabled) {
			mContactsButton.setVisibility(View.VISIBLE);
		} else {
			mContactsButton.setVisibility(View.GONE);
		}

		setUpViews();
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		dispatchOnHidden(hidden);
	}

	@Override
	public void onResume() {
		super.onResume();

		onHiddenChanged(isHidden());
	}

	@Override
	public void onPause() {
		super.onPause();

		dispatchOnHidden(true);
	}

	@Override
	public void onDestroyView() {
		mFriendsPage.destroy();
		mFriendsOnlinePage.destroy();

		if (mContactsPage != null) {
			mContactsPage.destroy();
		}

		super.onDestroyView();
	}

	@Override
	public void onFriendItemClick(AdapterView<?> adapterView, View view, int position, long id, Profile friend) {
		startActivity(ChatActivity.getDisplayIntent(getActivity(), friend));
	}

	protected void dispatchOnHidden(boolean hidden) {
		for (PageDescriptor page : mPages) {
			if (page.pageView instanceof FriendsPageView) {
				((FriendsPageView)page.pageView).onHiddenChanged(hidden);
			}
		}
	}

	@InjectOnClickListener(R.id.buttonFriends)
	protected void onFriendsButtonClicked(View view) {
		setDisplayedPage(mFriendsPage);
	}

	@InjectOnClickListener(R.id.buttonFriendsOnline)
	protected void onFriendsOnlineButtonClicked(View view) {
		setDisplayedPage(mFriendsOnlinePage);
	}

	@InjectOnClickListener(R.id.buttonContacts)
	protected void onContactsButtonClicked(View view) {
		if (mContactsPage == null) {
			mContactsPage = new ContactsPageView(getActivity(), null);
			mContactsPage.init();
			mPagesLayout.addView(mContactsPage, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			mPages.add(new PageDescriptor(mContactsButton, mContactsPage));
		}

		setDisplayedPage(mContactsPage);
	}

	private void setDisplayedPage(View pageView) {
		for (PageDescriptor page : mPages) {
			if (page.pageView == pageView) {
				page.tabView.setSelected(true);

				page.pageView.setVisibility(View.VISIBLE);
			} else {
				page.tabView.setSelected(false);

				page.pageView.setVisibility(View.GONE);
			}
		}
	}

	private void setUpViews() {
		Aibolit.doInjections(this, new VisibleInjectionContext(getView()));

		Activity activity = getActivity();

		if (activity instanceof OnFriendItemClickListener) {
			mOnFriendItemClickListener = (OnFriendItemClickListener)activity;
		} else {
			mOnFriendItemClickListener = this;
		}

		mFriendsPage.setOnFriendItemClickListener(mOnFriendItemClickListener);

		mFriendsOnlinePage.setOnFriendItemClickListener(mOnFriendItemClickListener);
		mFriendsOnlinePage.filterByAvailability(true);

		setDisplayedPage(mFriendsPage);
	}

	private static class PageDescriptor {
		public final TextView tabView;
		public final View pageView;

		public PageDescriptor(TextView tabView, View pageView) {
			this.tabView = tabView;
			this.pageView = pageView;
		}
	}
}
