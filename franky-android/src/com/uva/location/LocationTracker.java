package com.uva.location;

import java.util.Enumeration;

import com.uva.concurrent.ConditionalVariable;
import com.uva.concurrent.ObjectHolder;
import com.uva.utilities.AssertCompat;
import com.uva.utilities.ObserverCollection;

/**
 * Tracks changes of the location of the device, trying to determine better location.
 * <pre>
 * Behavior notes: 
 * - If at least one location listener appeared - it starts location tracking.
 * - If no location listeners left - it stops location tracking.
 * - It caches last fired location after first onLocationChange() was fired.
 * 
 * Implementation notes:
 * - You must implement startLocationTracking(), stopLocationTracking() methods.
 * - You can override getLastKnownLocation() method.
 * - You must implement location tracking in way when you not only track location changes but
 *   trying to determine better (more accurate) location.
 * </pre>
 * @author enikey.
 */
public abstract class LocationTracker {
	private final ObserverCollection<LocationListener> mLocationListeners = new ObserverCollection<LocationListener>();

	private Location mLastCachedLocation;

	/**
	 * Registers given listener for location changes.<br>
	 * If there cached location, notification will be received immediately.<br>
	 * Starts location tracking if this is first location listener.
	 * @param locationListener the location listener.
	 */
	public final synchronized void requestLocationUpdates(LocationListener locationListener) {
		mLocationListeners.add(locationListener);

		if (mLocationListeners.getCount() == 1) {
			startLocationTracking();
		}
	}

	/**
	 * Cancels location tracking for given listener.<br>
	 * Stops location tracking if no more listeners left.
	 * @param locationListener the location listener.
	 */
	public final synchronized void cancelLocationUpdates(LocationListener locationListener) {
		mLocationListeners.remove(locationListener);

		if (mLocationListeners.getCount() == 0) {
			stopLocationTracking();
		}
	}

	/**
	 * Returns last known location.<br>
	 * Current implementation returns last cached location.<br>
	 * Subclasses can override this method if different behavour is wanted.
	 * @return last known location or null if last known location is unknown.
	 */
	public Location getLastKnownLocation() {
		return getLastCachedLocation();
	}

	public Integer getDistanceToMe(Location location) {
		Location locationA = getLastKnownLocation();
		Location locationB = location;

		if (locationA == null || locationB == null) return null;

		return new Integer((int)Location.distanceBetweenConsideringAccuracy(locationA, locationB));
	}

	public Location getLocation(long timeoutMillis) throws InterruptedException {
		Location lastKnownLocation = getLastKnownLocation();

		final ObjectHolder determinedLocationWrapper = new ObjectHolder();

		final ConditionalVariable hasLocationCondition = new ConditionalVariable(lastKnownLocation != null);

		if (lastKnownLocation != null) {
			determinedLocationWrapper.setValue(lastKnownLocation);
		}

		LocationListener onLocationChange = new LocationListener() {
			@Override
			public void onLocationChange(Location newLocation) {
				determinedLocationWrapper.setValue(newLocation);
				hasLocationCondition.set(true);
			}
		};

		requestLocationUpdates(onLocationChange);

		try {
			if (hasLocationCondition.waitFor(true, timeoutMillis)) {
				return (Location)determinedLocationWrapper.getValue();
			} else {
				return null;
			}
		} finally {
			cancelLocationUpdates(onLocationChange);
		}
	}

	/**
	 * Returns last cached location.<br>
	 * <i>Note:</i>Last cached location automaticly sets up in onLocationChanged() method.
	 * @return last cached location or null if onLocationChanged() was never called.
	 */
	protected synchronized Location getLastCachedLocation() {
		return mLastCachedLocation;
	}

	/**
	 * Notifies location listeners with onLocationChange() notification and saves given location as cached.<br>
	 * Must be called from subclasses when location has changed or better location is available.
	 * @param newLocation the new location.
	 * @throws IllegalArgumentException when newLocation is null.
	 */
	protected final void onLocationChanged(Location newLocation) {
		AssertCompat.notNull(newLocation, "New location");

		synchronized (this) {
			mLastCachedLocation = newLocation;
		}

		notifyLocationChanged(newLocation);
	}

	protected abstract void startLocationTracking();

	protected abstract void stopLocationTracking();

	private void notifyLocationChanged(Location newLocation) {
		Enumeration<LocationListener> e = mLocationListeners.toEnumeration();

		while (e.hasMoreElements()) {
			e.nextElement().onLocationChange(newLocation);
		}
	}
}
