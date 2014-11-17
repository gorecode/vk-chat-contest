package com.uva.location;

import java.util.List;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.uva.log.Log;

public class AndroidLocationTracker extends LocationTracker {
	private static final int MIN_INTERVAL_FOR_NOTIFICATIONS = 3 * 60 * 1000;
	private static final int MIN_DISTANCE_FOR_NOTIFICATIONS = 50;

	private static final String TAG = "AndroidLocationTracker";

	private static final int THIRTY_MINUTES = 1000 * 60 * 30;

	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private final LocationManager mLocationManager;
	private final LocationReactor mReactor;
	private Location mCurrentBetterLocation;

	public AndroidLocationTracker(LocationManager manager) {
		assert manager != null;

		mReactor = new LocationReactor();
		mLocationManager = manager;
	}

	public boolean hasEnabledProviders() {
		List<String> providers = mLocationManager.getProviders(true);

		return providers.size() > 0;
	}

	@Override
	public com.uva.location.Location getLastKnownLocation() {
		if (mCurrentBetterLocation == null) {
			List<String> providers = mLocationManager.getAllProviders();
			Location lastKnownBetterLocation = null;
			for (String provider : providers) {
				Location lastKnownLocation = mLocationManager.getLastKnownLocation(provider);
				if ((lastKnownLocation != null) && (isBetterLocation(lastKnownLocation, lastKnownBetterLocation))) {
					lastKnownBetterLocation = lastKnownLocation;
				}
			}
			return toUvaLocation(lastKnownBetterLocation);
		} else {
			return super.getLastKnownLocation();
		}
	}

	protected void startLocationTracking() {
		final List<String> providers = mLocationManager.getAllProviders();

		mHandler.postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
				for (String provider : providers) {
					Log.debug(TAG, "Requesting location updates, provider = " + provider);

					mLocationManager.requestLocationUpdates(provider, MIN_INTERVAL_FOR_NOTIFICATIONS, MIN_DISTANCE_FOR_NOTIFICATIONS, mReactor);
				}
			}
		}); 
	}

	protected void stopLocationTracking() {
		mHandler.postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
				mLocationManager.removeUpdates(mReactor);
			}
		});
	}

	private static String ProviderStatusToString(int status) {
		switch (status) {
		case LocationProvider.AVAILABLE:
			return "Avaliable";
		case LocationProvider.OUT_OF_SERVICE:
			return "Out of service";
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			return "Temporary unavaliable";
		default:
			return "Unknown";
		}
	}

	/**
	 * Determines whether one Location reading is better than the current Location fix
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	protected static boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();

		boolean isNewer = timeDelta > 0;
		boolean isSignificantlyNewer = timeDelta > THIRTY_MINUTES;

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());

		boolean isNotLessAccurate = accuracyDelta <= 0;

		boolean isFromDifferentProviders = !isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		if (isFromDifferentProviders) {
			if (isNotLessAccurate) {
				return true;
			}
			if (isSignificantlyNewer) {
				return true;
			}
		} else {
			if (isNewer) {
				return true;
			}
		}

		return false;
	}

	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private static void logLocation(String name, Location location) {
		Log.debug(TAG, name + ": provider = " + location.getProvider() + ", lat = " + location.getLatitude() + ", lon = " + location.getLongitude() + ", accuracy = " + location.getAccuracy() + " meters, time = " + location.getTime());
	}

	private static com.uva.location.Location toUvaLocation(Location location) {
		if (location == null) return null;

		return new com.uva.location.Location(location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getTime());
	}

	private class LocationReactor implements LocationListener {
		@Override
		public void onLocationChanged(Location newLocation) {
			logLocation("New location", newLocation);

			List<String> enabledProviders = mLocationManager.getProviders(true);

			boolean newLocationProviderIsOnlyEnabledProvider = enabledProviders.size() == 1 && enabledProviders.get(0).equals(newLocation.getProvider());

			if (newLocationProviderIsOnlyEnabledProvider || isBetterLocation(newLocation, mCurrentBetterLocation)) {
				mCurrentBetterLocation = newLocation;
			} else {
				Log.debug(TAG, "Skipping new location");
				return;
			}

			logLocation("New better location", mCurrentBetterLocation);

			AndroidLocationTracker.this.onLocationChanged(toUvaLocation(mCurrentBetterLocation));
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.debug(TAG, "Provider disabled = " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.debug(TAG, "Provider enabled = " + provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.debug(TAG, "Provider = " + provider + ", status = " + ProviderStatusToString(status));
		}
	}
}
