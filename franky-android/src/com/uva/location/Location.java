package com.uva.location;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.uva.utilities.AssertCompat;

public final class Location implements Cloneable, Serializable {	
	private static final long serialVersionUID = -7465374114131233259L;
	
	public static final long TIMESTAMP_UNKNOWN = Long.MIN_VALUE;
	public static final double ACCURACY_UNKNOWN = -1;

	private static final String JSON_KEY_LATITUDE = "latitude";
	private static final String JSON_KEY_LONGITUDE = "longitude";
	private static final String JSON_KEY_ACCURACY = "accuracy";
	private static final String JSON_KEY_TIMESTAMP = "timestamp";

	public double latitude;
	public double longitude;
	public double accuracy;
	public long timestamp;

	public Location(Location location) {
		AssertCompat.notNull(location, "Source location");

		latitude = location.latitude;
		longitude = location.longitude;
		accuracy = location.accuracy;
		timestamp = location.timestamp;
	}

	public Location() {
		this(0, 0);
	}

	public Location(double latitude, double longitude) {
		this(latitude, longitude, ACCURACY_UNKNOWN, TIMESTAMP_UNKNOWN);
	}

	public Location(double latitude, double longitude, double accuracy) {
		this(latitude, longitude, accuracy, TIMESTAMP_UNKNOWN);
	}

	public Location(double latitude, double longitude, double accuracy, long timestamp) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
		this.timestamp = timestamp;

		normalize();
	}

	Location(JSONObject json) throws JSONException {
		this(
				json.getDouble(JSON_KEY_LATITUDE),
				json.getDouble(JSON_KEY_LONGITUDE),
				json.optDouble(JSON_KEY_ACCURACY, ACCURACY_UNKNOWN),
				json.optLong(JSON_KEY_TIMESTAMP, TIMESTAMP_UNKNOWN)
		);

		normalize();
	}

	public static Location fromJSON(JSONObject json) throws JSONException {
		return new Location(json);
	}

	@Override
	public Location clone() {
		try {
			return (Location) super.clone();
		} catch (CloneNotSupportedException e) {
			AssertCompat.shouldNeverHappen(e);
			return null;
		}
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put(JSON_KEY_LATITUDE, latitude);
			json.put(JSON_KEY_LONGITUDE, longitude);
		} catch (JSONException e) {
			AssertCompat.shouldNeverHappen(e);
		}
		return json;
	}

	public boolean hasAccuracy() {
		return accuracy >= 0;
	}

	public boolean hasTimestamp() {
		return timestamp != TIMESTAMP_UNKNOWN;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setLatitude(double latitude) {
		this.latitude = normalizeLatitude(latitude);
	}

	public void setLongitude(double longitude) {
		this.longitude = normalizeLongitude(longitude);
	}

	public void normalize() {
		latitude = normalizeLatitude(latitude);
		longitude = normalizeLongitude(longitude);
	}

	public String toString() {
		return latitude + " " + longitude;
	}

	public boolean equals(Object other) {
		if (other == null) return false;

		if (other instanceof Location) {
			Location otherLocation = (Location)other;

			return latitude == otherLocation.latitude && longitude == otherLocation.longitude && accuracy == otherLocation.accuracy && timestamp == otherLocation.timestamp;
		}

		return false;
	}

	public int hashCode() {
		int h1 = new Double(latitude).hashCode();
		int h2 = new Double(longitude).hashCode();
		int h3 = new Double(accuracy).hashCode();
		int h4 = new Long(timestamp).hashCode();

		return (h1^h2^h3^h4);
	}

	public double distanceTo(Location point) {
		return distanceBetween(this, point);
	}

	public static double distanceBetweenConsideringAccuracy(Location pos1, Location pos2) {
		double distanceBetweenCenters = distanceBetween(pos1, pos2);

		if (pos2.hasAccuracy()) {
			return Math.max(0, distanceBetweenCenters - pos2.accuracy);
		}

		return distanceBetweenCenters;
	}

	public static double distanceBetween(Location pos1, Location pos2) {
		float[] results = new float[1];
		distanceBetween(pos1.latitude, pos1.longitude, pos2.latitude, pos2.longitude, results);
		return results[0];
	}

	/**
   * Computes the approximate distance in meters between two
   * locations, and optionally the initial and final bearings of the
   * shortest path between them.  Distance and bearing are defined using the
   * WGS84 ellipsoid.
   *
   * <p> The computed distance is stored in results[0].  If results has length
   * 2 or greater, the initial bearing is stored in results[1]. If results has
   * length 3 or greater, the final bearing is stored in results[2].
   *
   * @param startLatitude the starting latitude
   * @param startLongitude the starting longitude
   * @param endLatitude the ending latitude
   * @param endLongitude the ending longitude
   * @param results an array of floats to hold the results
   *
   * @throws IllegalArgumentException if results is null or has length < 1
   */
  public static void distanceBetween(double startLatitude, double startLongitude,
      double endLatitude, double endLongitude, float[] results) {
      if (results == null || results.length < 1) {
          throw new IllegalArgumentException("results is null or has length < 1");
      }
      computeDistanceAndBearing(startLatitude, startLongitude,
          endLatitude, endLongitude, results);
  }

  private static void computeDistanceAndBearing(double lat1, double lon1,
      double lat2, double lon2, float[] results) {
      // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
      // using the "Inverse Formula" (section 4)

      int MAXITERS = 20;
      // Convert lat/long to radians
      lat1 *= Math.PI / 180.0;
      lat2 *= Math.PI / 180.0;
      lon1 *= Math.PI / 180.0;
      lon2 *= Math.PI / 180.0;

      double a = 6378137.0; // WGS84 major axis
      double b = 6356752.3142; // WGS84 semi-major axis
      double f = (a - b) / a;
      double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

      double L = lon2 - lon1;
      double A = 0.0;
      double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
      double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

      double cosU1 = Math.cos(U1);
      double cosU2 = Math.cos(U2);
      double sinU1 = Math.sin(U1);
      double sinU2 = Math.sin(U2);
      double cosU1cosU2 = cosU1 * cosU2;
      double sinU1sinU2 = sinU1 * sinU2;

      double sigma = 0.0;
      double deltaSigma = 0.0;
      double cosSqAlpha = 0.0;
      double cos2SM = 0.0;
      double cosSigma = 0.0;
      double sinSigma = 0.0;
      double cosLambda = 0.0;
      double sinLambda = 0.0;

      double lambda = L; // initial guess
      for (int iter = 0; iter < MAXITERS; iter++) {
          double lambdaOrig = lambda;
          cosLambda = Math.cos(lambda);
          sinLambda = Math.sin(lambda);
          double t1 = cosU2 * sinLambda;
          double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
          double sinSqSigma = t1 * t1 + t2 * t2; // (14)
          sinSigma = Math.sqrt(sinSqSigma);
          cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
          sigma = Math.atan2(sinSigma, cosSigma); // (16)
          double sinAlpha = (sinSigma == 0) ? 0.0 :
              cosU1cosU2 * sinLambda / sinSigma; // (17)
          cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
          cos2SM = (cosSqAlpha == 0) ? 0.0 :
              cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

          double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
          A = 1 + (uSquared / 16384.0) * // (3)
              (4096.0 + uSquared *
               (-768 + uSquared * (320.0 - 175.0 * uSquared)));
          double B = (uSquared / 1024.0) * // (4)
              (256.0 + uSquared *
               (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
          double C = (f / 16.0) *
              cosSqAlpha *
              (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
          double cos2SMSq = cos2SM * cos2SM;
          deltaSigma = B * sinSigma * // (6)
              (cos2SM + (B / 4.0) *
               (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                (B / 6.0) * cos2SM *
                (-3.0 + 4.0 * sinSigma * sinSigma) *
                (-3.0 + 4.0 * cos2SMSq)));

          lambda = L +
              (1.0 - C) * f * sinAlpha *
              (sigma + C * sinSigma *
               (cos2SM + C * cosSigma *
                (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

          double delta = (lambda - lambdaOrig) / lambda;
          if (Math.abs(delta) < 1.0e-12) {
              break;
          }
      }

      float distance = (float) (b * A * (sigma - deltaSigma));
      results[0] = distance;
      if (results.length > 1) {
          float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
              cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
          initialBearing *= 180.0 / Math.PI;
          results[1] = initialBearing;
          if (results.length > 2) {
              float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                  -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
              finalBearing *= 180.0 / Math.PI;
              results[2] = finalBearing;
          }
      }
  }
	public static double normalizeLatitude(double latitude) {
		if (latitude > 90.0 || latitude < -90.0) {
			return latitude % 90.0;
		}
		return latitude;
	}

	public static double normalizeLongitude(double longitude) {
		if (longitude > 180.0 || longitude < -180.0) {
			return longitude % 180.0;
		}
		return longitude;
	}

	public static Location parseLocation(String string) {
		if (string == null) {
			throw new IllegalArgumentException("Location string cannot be null");
		}

		int delimPos = string.indexOf(' ');

		try {
			String latString = string.substring(0, delimPos);
			String lonString = string.substring(delimPos + 1, string.length());

			return new Location(Double.parseDouble(latString), Double.parseDouble(lonString));
		} catch (RuntimeException e) {
			;
		}

		throw new IllegalArgumentException("Location must be in 'latitude longitude' format");
	}
}
