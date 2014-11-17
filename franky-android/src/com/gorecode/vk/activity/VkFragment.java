package com.gorecode.vk.activity;

import android.os.Bundle;
import android.view.View;

import com.google.common.base.Function;
import com.gorecode.vk.task.AutoCancelPool;
import com.uva.utilities.ObserverCollection;

import roboguice.fragment.RoboFragment;

public class VkFragment extends RoboFragment {
	private boolean mOnDestroyViewCalled;

	private final AutoCancelPool mAutoCancelPool = new AutoCancelPool();
	private final ObserverCollection<FragmentCallbacks> mObservers = new ObserverCollection<FragmentCallbacks>();

	public void registerCallbacks(FragmentCallbacks callbacks) {
		mObservers.add(callbacks);
	}

	public void unregisterCallbacks(FragmentCallbacks callbacks) {
		mObservers.remove(callbacks);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mObservers.callForEach(new Function<FragmentCallbacks, Void>() {
			@Override
			public Void apply(FragmentCallbacks arg) {
				arg.onFragmentCreated(VkFragment.this, savedInstanceState);

				return null;
			}
		});
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mObservers.callForEach(new Function<FragmentCallbacks, Void>() {
			@Override
			public Void apply(FragmentCallbacks arg) {
				arg.onFragmentViewCreated(VkFragment.this, view, savedInstanceState);

				return null;
			}
		});

	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mOnDestroyViewCalled = true;

		mAutoCancelPool.drain();

		mObservers.callForEach(new Function<FragmentCallbacks, Void>() {
			@Override
			public Void apply(FragmentCallbacks arg) {
				arg.onFragmentViewDestroyed(VkFragment.this);

				return null;
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mObservers.callForEach(new Function<FragmentCallbacks, Void>() {
			@Override
			public Void apply(FragmentCallbacks arg) {
				arg.onFragmentDestroyed(VkFragment.this);

				return null;
			}
		});
	}

	public AutoCancelPool getAutoCancelPool() {
		return mAutoCancelPool;
	}

	public boolean shouldNotTouchViews() {
		return isDetached() || isRemoving() || isViewDestroyed();
	}

	public boolean isViewDestroyed() {
		return mOnDestroyViewCalled;
	}
}
