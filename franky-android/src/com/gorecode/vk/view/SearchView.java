package com.gorecode.vk.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.gorecode.vk.R;
import com.google.common.base.Strings;

public class SearchView extends RelativeLayout {
	private static final int NO_SEARCH_HINT = -1;

	public static abstract class OnQueryTextListenerAdapter implements TextWatcher, TextView.OnEditorActionListener, OnQueryTextListener {
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				if (v.getText() != null) {
					onQueryTextSubmit(v.getText().toString());
					return true;
				}
			}
			return false;
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			;
		}

		@Override
		public void afterTextChanged(Editable s) {
			onQueryTextChange(s.toString());
		}
	}

	public static interface OnQueryTextListener {
		public boolean onQueryTextChange(String query);
		public boolean onQueryTextSubmit(String query);
	}

	private View deleteButton;
	private ViewAnimationHelper deleteButtonAnimationController;

	private SearchAutoCompleteTextView edit;

	private OnQueryTextListener onQueryTextListener;

	public SearchView(Context context) {
		this(context, null);
	}

	public SearchView(Context context, AttributeSet attrs) {
		super(context, attrs);

		inflate(context, R.layout.search_bar, this);

		if (isInEditMode()) return;

		edit = (SearchAutoCompleteTextView)this.findViewById(R.id.editSearch);
		deleteButton = findViewById(R.id.delete_button);
		deleteButton.setVisibility(edit.getText().toString().length() == 0 ? GONE : VISIBLE);

		if (attrs != null) {
			setStyledAttributes(context, attrs);
		}

		edit.addTextChangedListener(editEventHandler);
		edit.setOnEditorActionListener(editEventHandler);

		if (isInEditMode()) {
			return;
		}

		deleteButton.setOnClickListener(onClickListener);
		deleteButtonAnimationController = new ViewAnimationHelper(deleteButton, Animations.DEFAULT_HIDE_ANIMATION, Animations.DEFAULT_SHOW_ANIMATION);
	}

	public AutoCompleteTextView getQueryEdit() {
		return edit;
	}

	public void setOnQueryTextListener(OnQueryTextListener listener) {
		onQueryTextListener = listener;
	}

	public String getText() {
		return edit.getText().toString();
	}

	public void addTextChangedListener(TextWatcher onSearchEditChanged) {
		edit.addTextChangedListener(onSearchEditChanged);
	}

	public void setOnEditorActionListener(OnEditorActionListener onSearchEditActionPerformed) {
		edit.setOnEditorActionListener(onSearchEditActionPerformed);
	}

	private void setStyledAttributes(Context context, AttributeSet attrs) {
		TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.SearchView);

		if (!isInEditMode()) {
			int searchHintId = styledAttrs.getResourceId(R.styleable.SearchView_searchHint, NO_SEARCH_HINT);
			if (searchHintId != NO_SEARCH_HINT) {
				edit.setHint(getResources().getString(styledAttrs.getResourceId(R.styleable.SearchView_searchHint, 0)));
			}
		}
		styledAttrs.recycle();
	}

	private final OnQueryTextListenerAdapter editEventHandler = new OnQueryTextListenerAdapter() {
		@Override
		public boolean onQueryTextSubmit(String query) {
			if (onQueryTextListener != null) onQueryTextListener.onQueryTextSubmit(query);

			return false;
		}

		@Override
		public boolean onQueryTextChange(String query) {
			if (onQueryTextListener != null) onQueryTextListener.onQueryTextChange(query);

			if (Strings.isNullOrEmpty(query)) {
				deleteButtonAnimationController.hideView();
			} else {
				deleteButtonAnimationController.showView();
			}

			return false;
		}
	};

	private final View.OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v == deleteButton) {
				edit.setText("");
			}
		}
	};
}
