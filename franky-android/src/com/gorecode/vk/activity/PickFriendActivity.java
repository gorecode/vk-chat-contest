package com.gorecode.vk.activity;

import roboguice.inject.ContentView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.gorecode.vk.R;
import com.gorecode.vk.activity.friends.FriendsFragment;
import com.gorecode.vk.activity.friends.FriendsPageView.OnFriendItemClickListener;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.utilities.BundleUtilities;

@ContentView(R.layout.pick_friend_activity)
public class PickFriendActivity extends VkFragmentActivity implements OnFriendItemClickListener {
	public static Profile getActivityResult(Intent data) {
		return BundleUtilities.getProfile(data.getExtras(), EXTRA_PERSON);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_BOTTOM);

		setResult(RESULT_CANCELED);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().replace(R.id.content, FriendsFragment.newInstance(this, true)).commit();
		}
	}

	@Override
	public void onFriendItemClick(AdapterView<?> adapterView, View view, int position, long id, Profile friend) {
		Intent data = new Intent();
		BundleUtilities.putExtra(data, EXTRA_PERSON, friend);
		setResult(RESULT_OK, data);
		finish();
	}
}
