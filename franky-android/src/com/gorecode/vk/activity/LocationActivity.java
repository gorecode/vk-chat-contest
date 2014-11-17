package com.gorecode.vk.activity;

import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.inject.Inject;
import com.gorecode.vk.activity.map.PointOverlay;
import com.gorecode.vk.utilities.MapUtilities;
import com.uva.location.Location;
import com.uva.location.LocationTracker;

public class LocationActivity extends VkMapActivity {
	private static final String EXTRA_LATITUDE = "latitude";
	private static final String EXTRA_LONGITUDE = "longitude";

	@InjectView(R.id.pick_location_button)
	private Button mPickLocationButton;
	@InjectView(R.id.mapView)
	private MapView mMapView;

	@Inject
	private LocationTracker mLocTracker;

	private MyLocationOverlay mMyLocationOverlay;
	private PointOverlay mPointOverlay;

	private GeoPoint mPoint;

	public static Intent getDisplayIntent(Context context, Location point) {
		Intent i = new Intent(context, LocationActivity.class);
		i.putExtra(EXTRA_LATITUDE, point.latitude);
		i.putExtra(EXTRA_LONGITUDE, point.longitude);
		return i;
	}

	public static Location getResult(Intent data) {
		if (data.hasExtra(EXTRA_LATITUDE) && data.hasExtra(EXTRA_LONGITUDE)) {
			double latitude = data.getDoubleExtra(EXTRA_LATITUDE, 0);
			double longitude = data.getDoubleExtra(EXTRA_LONGITUDE, 0);
			return new Location(latitude, longitude);
		}
		return null; 
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setAnimations(ANIMATIONS_SLIDE_BOTTOM);

		setContentView(R.layout.location_activity);

		setResult(RESULT_CANCELED);
		
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);

		mPointOverlay = new PointOverlay(getResources().getDrawable(R.drawable.map_location)) {
			@Override
			public boolean onTap(GeoPoint geoPoint, MapView mapView) {
				if (isInPickMode()) {
					mPoint = geoPoint;

					setLocation(geoPoint);

					updateViews();

					return true;
				} else {
					return false;
				}
			}
		};

		if (isInPickMode()) {
			mMapView.getOverlays().add(mMyLocationOverlay);

			Location lastKnownLocation = mLocTracker.getLastKnownLocation();

			if (lastKnownLocation != null) {
				int latE6 = (int)(lastKnownLocation.latitude * 1000000.0D);
				int lonE6 = (int)(lastKnownLocation.longitude * 1000000.0D);

				GeoPoint geoPoint = new GeoPoint(latE6, lonE6);

				mMapView.getController().setCenter(geoPoint);

				mPointOverlay.setLocation(lastKnownLocation.latitude, lastKnownLocation.longitude);

				mPoint = geoPoint;
			}

			mPickLocationButton.setVisibility(View.VISIBLE);
		} else {
			Bundle extras = getIntent().getExtras();

			mPoint = MapUtilities.createGeoPoint(extras.getDouble(EXTRA_LATITUDE), extras.getDouble(EXTRA_LONGITUDE));

			mPointOverlay.setLocation(mPoint);

			mPickLocationButton.setVisibility(View.GONE);

			mMapView.getController().setCenter(mPoint);
		}

		mMapView.setBuiltInZoomControls(true);
		mMapView.getController().setZoom(6);
		mMapView.getOverlays().add(mPointOverlay);

		updateViews();

		Aibolit.doInjections(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		mMyLocationOverlay.enableMyLocation();
	}

	@Override
	public void onPause() {
		super.onPause();

		mMyLocationOverlay.disableMyLocation();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@InjectOnClickListener(R.id.pick_location_button)
	protected void onPickLocationButtonClick(View v) {
		setResult(RESULT_OK, new Intent().putExtra(EXTRA_LATITUDE, mPoint.getLatitudeE6() / (double)1e+6).putExtra(EXTRA_LONGITUDE,  mPoint.getLongitudeE6() / (double)1e+6));

		finish();
	}

	private boolean isInPickMode() {
		return Intent.ACTION_PICK.equals(getIntent().getAction());
	}

	private void updateViews() {
		mPickLocationButton.setEnabled(mPoint != null);
	}
}
