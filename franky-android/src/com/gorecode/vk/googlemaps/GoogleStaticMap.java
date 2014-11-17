package com.gorecode.vk.googlemaps;

import android.content.Context;
import android.net.Uri;

import com.google.inject.Inject;
import com.gorecode.vk.data.Size;
import com.uva.lang.StringUtilities;
import com.uva.location.Location;

public class GoogleStaticMap {
	public static final int ZOOM_MINIMAL = 0;
	public static final int ZOOM_MIDDLE = 10;
	public static final int ZOOM_MAXIMAL = 19;

	public static final String MAP_TYPE_ROADMAP = "roadmap";
	public static final String MAP_TYPE_MOBILE = "mobile";
	public static final String MAP_TYPE_SATELLITE = "satellite";

	private static final String PARAMETER_API_KEY = "key";
	private static final String PARAMETER_CENTER = "center";
	private static final String PARAMETER_ZOOM = "zoom";
	private static final String PARAMETER_IMAGE_SIZE = "size";
	private static final String PARAMETER_MARKERS = "markers";
	private static final String PARAMETER_MAP_TYPE = "maptype";
	
	private String apiKey;
	private String mapType;
	
	@Inject
	public GoogleStaticMap(Context unused) {
		this.apiKey = Constants.KEY;
	}

	public void setMapType(String mapType) {
		this.mapType = mapType;
	}

	public String getUrlForImage(Size imageSize, Location center, int zoom) {
		Uri.Builder builder = createUrlBuilder();
		builder.appendQueryParameter(PARAMETER_IMAGE_SIZE, QueryParameters.toQueryValue(imageSize));
		builder.appendQueryParameter(PARAMETER_CENTER, QueryParameters.toQueryValue(center));
		builder.appendQueryParameter(PARAMETER_ZOOM, String.valueOf(zoom));
		if (mapType != null) {
			builder.appendQueryParameter(PARAMETER_MAP_TYPE, mapType);
		}
		return builder.build().toString();
	}

	public String getUrlForImage(Size imageSize, Marker marker) {
		return getUrlForImage(imageSize, new Marker[] { marker } );
	}

	public String getUrlForImage(Size imageSize, Marker[] markers) {
		Uri.Builder builder = createUrlBuilder();
		builder.appendQueryParameter(PARAMETER_IMAGE_SIZE, QueryParameters.toQueryValue(imageSize));
		builder.appendQueryParameter(PARAMETER_MARKERS, QueryParameters.toQueryValue(markers));
		if (mapType != null) {
			builder.appendQueryParameter(PARAMETER_MAP_TYPE, mapType);
		}
		return builder.build().toString();
	}

	private Uri.Builder createUrlBuilder() {
		return new Uri.Builder().scheme("http").authority("maps.google.com").path("staticmap").appendQueryParameter(PARAMETER_API_KEY, apiKey);
	}

	static class QueryParameters {
		public static String toQueryValue(Marker marker) {
			String formatted = toQueryValue(marker.location);

			if (marker.color != null) {
				formatted += "," + marker.color;
			}

			if (marker.alphaCharacter != null) {
				if (marker.color == null) {
					formatted += ",";
				}
				formatted += marker.alphaCharacter;
			}

			return formatted;
		}

		public static String toQueryValue(Marker[] markers) {
			String[] strings = new String[markers.length];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = toQueryValue(markers[i]);
			}
			return StringUtilities.join("|", strings);
		}

		public static String toQueryValue(Size size) {
			return String.format("%1$dx%2$d", size.width, size.height);
		}

		public static String toQueryValue(Location location) {
			return String.format("%1$s,%2$s", toQueryValue(location.latitude), toQueryValue(location.longitude));
		}

		public static String toQueryValue(double value) {
			return String.valueOf(value).replace(',', '.');
		}
	}
}
