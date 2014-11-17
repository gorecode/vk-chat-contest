package com.gorecode.vk.utilities;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.TextView;

import com.uva.location.Location;
import com.uva.log.Log;

public class GeocodeUtilities {
	private static final String TAG = "GeocodeUtilities";

	public interface GeocodeListener {
		public void onResult(String result);
		public void onFail(Exception e);
	}

	public static void getGeodingInfo(final Context context, final Location uvaLocation, final boolean onlyCountry, final GeocodeListener listener) {
		Thread gcThread = new Thread(new Runnable() {
			public void run() {
				Exception exc = null; 
				Geocoder gc = new Geocoder(context, Locale.getDefault());
				Address adr = null;
				try {
					List<Address> adrList= gc.getFromLocation(uvaLocation.getLatitude(),uvaLocation.getLongitude(), 1);
					if(adrList.size() > 0)
						adr = adrList.get(0);
				} catch (IOException e) {
					exc = e;
					Log.exception(TAG, e);
				} catch (Exception e) {
					exc = e;
					Log.exception(TAG, e);
				}

				if(adr != null) {
					if (onlyCountry) {
						listener.onResult(adr.getCountryCode());
					} else {
						StringBuilder builder = new StringBuilder();
						builder.append(adr.getLocality() != null ? adr.getLocality(): "");
						builder.append(adr.getAdminArea() != null ? (builder.length() > 0? ", ": "")+ adr.getAdminArea(): "");
						builder.append((builder.length() > 0? ", ": "") + adr.getCountryName());
						listener.onResult(builder.toString());
					}
				} else {
					listener.onFail(exc);	
				}

			}
		});		
		gcThread.start();
	}

	public static void outputGeodingInfo(final Activity contextActivity,final Location uvaLocation, final TextView outputView, final boolean onlyCountry, final GeocodeListener subListener) {
		GeocodeListener listener = new GeocodeListener() {
			public void onResult(final String result) {
				if(subListener != null)
					subListener.onResult(result);
				if (!onlyCountry) {
					contextActivity.runOnUiThread(new Runnable() {
						public void run() {
							outputView.setText(result);
						}
					});
				}
			}

			public void onFail(Exception e) {
				String locFormatted= String.format("lat:%.4f lon:%.4f", uvaLocation.getLatitude(), uvaLocation.getLongitude()); 
				if(subListener != null)
					subListener.onResult(locFormatted);
				if(onlyCountry)
					return;
				final String outString = locFormatted;
				contextActivity.runOnUiThread(new Runnable() {
					public void run() {
						outputView.setText(outString);
					}
				});	
			}
		};

		getGeodingInfo(contextActivity, uvaLocation, onlyCountry, listener);
	}
}
