package com.gorecode.vk.activity.map;

import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import com.gorecode.vk.utilities.MapUtilities;

public class PointOverlay extends ItemizedOverlay<OverlayItem> {
	private OverlayItem item;
	
	public PointOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		populate();
	}
	
	public void setLocation(GeoPoint p) {
		item = new OverlayItem(p, null, null);
		populate();		
	}

	public void setLocation(double latitude, double longitude) {
		item = new OverlayItem(MapUtilities.createGeoPoint(latitude, longitude), null, null);
		populate();
	}
	
	public GeoPoint getGeoPoint() {
		if (item == null) return null;
		return item.getPoint();
	}
	
	public void clear() {
		setLastFocusedIndex(-1);
		item = null;
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return item;
	}

	@Override
	public int size() {
		return (item == null ? 0 : 1);
	}
}
