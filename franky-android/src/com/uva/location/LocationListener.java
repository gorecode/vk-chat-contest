package com.uva.location;

/**
 * Used for receiving notifications from the LocationTracker when the location has changed or 
 * better location is avaliable.
 * @author enikey.
 */
public interface LocationListener {
	/**
	 * Called when location has changed or better location has determined.
	 * @param newLocation the new location.
	 */
	public void onLocationChange(Location newLocation);
}
