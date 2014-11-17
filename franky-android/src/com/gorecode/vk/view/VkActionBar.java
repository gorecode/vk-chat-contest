package com.gorecode.vk.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gorecode.vk.R;

public class VkActionBar extends RelativeLayout {
	private ImageView mBackButton;
	private TextView mTitleView;
	private ProgressBar mIntermediateProgressBar;
	private ViewGroup mActionButtonLayout;

	private int mPendingIntermediateTaskCount;

	public VkActionBar(final Context context, AttributeSet attrs) {
		super(context, attrs);

		inflate(context, R.layout.action_bar, this);

		mIntermediateProgressBar = (ProgressBar)findViewById(R.id.progress);
		mBackButton = (ImageView)findViewById(R.id.back_button);
		mTitleView = (TextView)findViewById(R.id.title);
		mActionButtonLayout = (ViewGroup)findViewById(R.id.action_button_layout);

		TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ActionBar);

		int resId = attributes.getResourceId(R.styleable.ActionBar_title, -1);

		if (resId != -1) {
			mTitleView.setText(context.getString(resId));
		}

		boolean backButtonVisible = attributes.getBoolean(R.styleable.ActionBar_backButton_visible, true);

		mBackButton.setVisibility(backButtonVisible ? VISIBLE : GONE);
		mBackButton.setOnClickListener(mOnBackClicked);

		mIntermediateProgressBar.setVisibility(backButtonVisible ? GONE : INVISIBLE);

		int actionButtonLayout = attributes.getResourceId(R.styleable.ActionBar_actionButton_layout, -1);
		int actionButtonImage = -1;

		if (actionButtonLayout != -1) {
			inflate(context, actionButtonLayout, mActionButtonLayout);
		} else {
			actionButtonImage = attributes.getResourceId(R.styleable.ActionBar_actionButton_image, -1);

			if (actionButtonImage != -1) {
				ImageView imageView = new ImageView(context);
				imageView.setImageResource(actionButtonImage);
				mActionButtonLayout.addView(imageView);
			}
		}

		mActionButtonLayout.setVisibility(attributes.getBoolean(R.styleable.ActionBar_actionButton_visible, actionButtonLayout != -1 || actionButtonImage != -1) ? VISIBLE : GONE);

		attributes.recycle();

		if (isInEditMode()) return;

		mIntermediateProgressBar.setVisibility(GONE);

		updateViews();
	}

	public void setTitle(String title) {
		mTitleView.setText(title);
	}

	public void setActionButtonOnClickListener(View.OnClickListener listener) {
		mActionButtonLayout.setOnClickListener(listener);
	}

	public void setActionButtonVisibility(int visibility) {
		mActionButtonLayout.setVisibility(visibility);
	}

	public void incrementIntermediateTasksCount() {
		mPendingIntermediateTaskCount++;

		updateViews();
	}

	public void decrementIntermediateTasksCount() {
		mPendingIntermediateTaskCount--;

		updateViews();
	}

	private void updateViews() {
		if (mBackButton.getVisibility() == View.GONE) {
			mIntermediateProgressBar.setVisibility(mPendingIntermediateTaskCount > 0 ? VISIBLE : INVISIBLE);
		}
	}

	private final View.OnClickListener mOnBackClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (getContext() instanceof Activity) {
				((Activity)getContext()).finish();
			}
		}
	};
}

