package com.gorecode.vk.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

public class ViewUtilities {
	public static void hideSoftInput(View view) {
		InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static int dipsToPixels(Context context, float dip) {
		// Get the screen's density scale
		final float scale = context.getResources().getDisplayMetrics().density;
		// Convert the dps to pixels, based on density scale
		return (int)(dip * scale + 0.5f);
	}

	public static void showAllChildrenExcept(ViewGroup group, View... exceptions) {
		setVisibilityToAllChildrenExcept(group, View.VISIBLE, exceptions);
	}

	public static void hideAllChildrenExcept(ViewGroup group, View... exceptions) {
		setVisibilityToAllChildrenExcept(group, View.GONE, exceptions);
	}

	public static void setVisibilityToAllChildrenExcept(ViewGroup group, int visibility, View... exceptions) {
		for (int i = 0; i < group.getChildCount(); i++) {
			View child = group.getChildAt(i);

			boolean shouldApplyVisibility = true;

			for (View exception : exceptions) {
				if (exception == child) {
					shouldApplyVisibility = false;
					break;
				}
			}

			if (shouldApplyVisibility) {
				child.setVisibility(visibility);
			}
		}
	}

	public static void setPaddingRight(View view, int right) {
		final int left = view.getPaddingLeft();
		final int top = view.getPaddingTop();
		final int bottom = view.getPaddingBottom();

		view.setPadding(left, top, right, bottom);
	}

	public static int getChildIndex(ViewGroup parent, View child) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			if (parent.getChildAt(i) == child) return i;
		}
		// FIXME: Why 0?
		return 0;
	}

	private ViewUtilities() {
		;
	}
}
