package com.gorecode.vk.utilities;

import com.google.android.maps.GeoPoint;

public class MapUtilities {
	public static int limitedZoom(int originalZoom, boolean isSatellite) {
		// int maxZoomLevel = mMapView.getMaxZoomLevel(); // wtf???how come black-out is acceptable zoom level??? ->doesn't work		
		// XXX obviously hack, problems IF zoom level values change
		int maxZoomLevel = isSatellite?20:22;
		int minZoomLevel = 2;
		if (originalZoom > maxZoomLevel)
			return maxZoomLevel;
		if(originalZoom < minZoomLevel)
			return minZoomLevel;
		return originalZoom;
	}
	
	public static GeoPoint createGeoPoint(double latitude, double longitude) {
		return new GeoPoint((int)(latitude * 1e+6), (int)(longitude * 1e+6));
	}
	
	public static GeoPoint createGeoPoint(android.location.Location l) {
		return createGeoPoint(l.getLatitude(), l.getLongitude());
	}	
	
	public static GeoPoint createGeoPoint(com.uva.location.Location l) {  
		return createGeoPoint(l.getLatitude(), l.getLongitude());
	}
}
