package com.gorecode.vk.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class SingleViewAdapter extends BaseAdapter {

	private int viewType;
	private int resource;
	private Context context;
	
	public SingleViewAdapter(Context context, int resource, int viewType) {
		this.viewType = viewType;
		this.resource = resource;
		this.context = context;
	}
	
	@Override
	public int getItemViewType(int position) {
	
		return viewType;
	}
	
	@Override
	public int getCount() {
		
		return 1;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(resource, null);
		}
		return convertView;
	}
	
	
}