package com.gorecode.vk.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.gorecode.vk.R;

class EndlessListAdapter extends AdapterWrapper {
	public static final int PLACEMENT_BOTTOM = 0x0;
	public static final int PLACEMENT_TOP = 0x1;

	public static final int DISPLAYMENT_NO_MIXIN = 0x0;
	public static final int DISPLAYMENT_ERROR = 0x1;
	public static final int DISPLAYMENT_LOADING = 0x2;

	private int mixinDisplayment;
	private int mixinPlacement;
	private Context context;
	private int pendingResource = -1;
	private int errorResource = -1;

	public EndlessListAdapter(Context context, ListAdapter wrapped) {
		this(context, wrapped, R.layout.item_loading, R.layout.item_error);
	}

	public EndlessListAdapter(Context context, ListAdapter wrapped, int pendingResource, int errorResource) {
		this(context, wrapped, PLACEMENT_BOTTOM, pendingResource, errorResource);
	}

	public EndlessListAdapter(Context context, ListAdapter wrapped, int mixinPlacement, int pendingResource, int errorResource) {
		super(wrapped);

		this.mixinPlacement = mixinPlacement;
		this.context = context;
		this.pendingResource = pendingResource;
		this.errorResource = errorResource;
	}

	public boolean isMergedViewAtTop() {
		return mixinPlacement == PLACEMENT_TOP;
	}

	public boolean isMergedViewAtBottom() {
		return mixinPlacement == PLACEMENT_BOTTOM;
	}

	public boolean isPendingViewVisible() {
		return mixinDisplayment == DISPLAYMENT_LOADING;
	}

	public void setErrorResource(int resId) {
		errorResource = resId;
	}

	public void setPendingResource(int resId) {
		pendingResource = resId;
	}

	public void setMixinPlacement(int placement) {
		mixinPlacement = placement;

		notifyDataSetChanged();
	}

	public void setMixinDisplayment(int displayment) {
		mixinDisplayment = displayment;

		notifyDataSetChanged();
	}

	public int getMixinDisplayment() {
		return mixinDisplayment;
	}

	 @Override
	 public Object getItem(int position) {
		 if (isMixin(position)) return "MixedView";

		 return super.getItem(getWrappedPosition(position));
	 }

	 @Override
	 public boolean areAllItemsEnabled() {
		 return false;
	 }

	@Override
	public boolean isEnabled(int position) {
		if (isMixin(position)) return false;

		return super.isEnabled(getWrappedPosition(position));
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public int getCount() {
		if (shouldDisplayMixin()) {
			return super.getCount() + 1;
		}

		return super.getCount();
	}

	@Override
	public long getItemId(int position) {
		if (isMixin(position)) return -1;

		return super.getItemId(getWrappedPosition(position));
	}

	@Override
	public int getItemViewType(int position) {
		if (isMixin(position)) {
			return IGNORE_ITEM_VIEW_TYPE;
		}

		return super.getItemViewType(getWrappedPosition(position));
	}

	@Override
	public int getViewTypeCount() {
		return super.getViewTypeCount() + 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;

		if (isMixin(position)) {
			if (mixinDisplayment == DISPLAYMENT_ERROR) {
				view = getErrorView(parent);
			}

			if (mixinDisplayment == DISPLAYMENT_LOADING) {
				view = getPendingView(parent);
			}
		} else {
			int wrappedPosition = getWrappedPosition(position);

			view = super.getView(wrappedPosition, convertView, parent);
		}

		return view;
	}

	protected View getPendingView(ViewGroup parent) {
		if (context != null) {
			return inflatePendingView(parent);
		}

		throw new RuntimeException("You must either override getPendingView() or supply a pending View resource via the constructor");
	}

	protected View getErrorView(ViewGroup parent) {
		if (context != null) {
			return inflateErrorView(parent);
		}

		throw new RuntimeException("You must either override getErrorView() or supply a pending View resource via the constructor");
	}

	protected View inflatePendingView(ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(pendingResource, parent, false);

		view.setClickable(true);

		return view;		
	}

	protected View inflateErrorView(ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View errorView = inflater.inflate(errorResource, parent, false);

		return errorView;
	}

	protected Context getContext() {
		return(context);
	}

	private int getMixinPosition() {
		if (mixinPlacement == PLACEMENT_BOTTOM) return super.getCount();

		return 0;
	}

	private int getWrappedPosition(int position) {
		if (shouldDisplayMixin()) {
			if (mixinPlacement == PLACEMENT_BOTTOM) return position;

			return position - 1;
		}

		return position;
	}

	private boolean isMixin(int position) {
		return shouldDisplayMixin() && position == getMixinPosition();
	}

	private boolean shouldDisplayMixin() {
		return (mixinDisplayment != DISPLAYMENT_NO_MIXIN);
	}
}