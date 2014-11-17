package com.gorecode.vk.view;

import android.view.View;
import android.view.animation.Animation;

public class ViewAnimationHelper {
	public static final int HIDE_METHOD_SET_GONE = 0x0;
	public static final int HIDE_METHOD_SET_INVISIBLE = 0x1;

	private static final int DEFAULT_HIDE_METHOD = HIDE_METHOD_SET_GONE;

	private View mView;

	private Animation mHideAnimation;
	private Animation mShowAnimation;

	private boolean mHideIsPending;
	private boolean mHideIsComplete;

	private boolean mShowIsPending;
	private boolean mShowIsComplete;

	private int mHideMethod = DEFAULT_HIDE_METHOD;

	public ViewAnimationHelper(View view) {
		mView = view;
	}

	public ViewAnimationHelper(View view, Animation hideAnimation, Animation showAnimation) {
		this(view, hideAnimation, showAnimation, DEFAULT_HIDE_METHOD);
	}

	public ViewAnimationHelper(View view, Animation hideAnimation, Animation showAnimation, int hideMethod) {
		this(view);

		setHideAnimation(hideAnimation);
		setShowAnimation(showAnimation);
		setHideMethod(hideMethod);
	}

	public void setHideMethod(int hideMethod) {
		mHideMethod = hideMethod;
	}

	public void setAnimationsDuration(long durationMillis) {
		mHideAnimation.setDuration(durationMillis);
		mShowAnimation.setDuration(durationMillis);
	}

	public void setShowAnimation(Animation animation) {
		mShowAnimation = animation;
	}

	public void setHideAnimation(Animation animation) {
		mHideAnimation = animation;
	}

	public boolean showView() {
		if (mShowIsPending || mShowIsComplete) return false;

		mView.removeCallbacks(mCompleteHideAnimation);
		mView.clearAnimation();
		mView.startAnimation(mShowAnimation);
		mView.setVisibility(View.VISIBLE);
		mView.postDelayed(mCompleteShowAnimation, mShowAnimation.getDuration());

		mShowIsPending = true;
		mShowIsComplete = false;

		mHideIsComplete = false;
		mHideIsPending = false;

		return true;
	}

	public boolean hideView() {
		if (mHideIsPending || mHideIsComplete || mView.getVisibility() != View.VISIBLE) return false;

		mView.clearAnimation();
		mView.startAnimation(mHideAnimation);
		mView.removeCallbacks(mCompleteShowAnimation);
		mView.postDelayed(mCompleteHideAnimation, mHideAnimation.getDuration());

		mHideIsPending = true;
		mHideIsComplete = false;

		mShowIsComplete = false;
		mShowIsPending = false;

		return true;
	}

	private final Runnable mCompleteShowAnimation = new Runnable() {
		@Override
		public void run() {
			mShowIsPending = false;
			mShowIsComplete = true;
		}
	};

	private final Runnable mCompleteHideAnimation = new Runnable() {
		@Override
		public void run() {
			if (mHideMethod == HIDE_METHOD_SET_GONE) {
				mView.setVisibility(View.GONE);
			}
			if (mHideMethod == HIDE_METHOD_SET_INVISIBLE) {
				mView.setVisibility(View.INVISIBLE);
			}
			mHideIsComplete = true;
			mHideIsPending = false;
		}
	};
}
