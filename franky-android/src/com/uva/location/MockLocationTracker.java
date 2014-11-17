package com.uva.location;

public class MockLocationTracker extends LocationTracker {
	private boolean mIsStarted;

	public void setLocation(Location newLocation) {
		if (newLocation != null) {
			onLocationChanged(newLocation);
		}
	}

	public boolean isStarted() {
		return mIsStarted;
	}

	protected void startLocationTracking() {
		mIsStarted = true;
	}

	protected void stopLocationTracking() {
		mIsStarted = false;
	}
}
