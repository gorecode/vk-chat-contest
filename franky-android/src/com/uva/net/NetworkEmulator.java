package com.uva.net;

import java.io.IOException;
import java.util.Random;

import com.uva.log.Log;

public class NetworkEmulator {
	public static class Quality {
		public static final int IDEAL = 0x0;
		public static final int LAN = 0x1;
		public static final int WIFI = 0x3;
		public static final int CELL = 0x4;
		public static final int GPRS = 0x5;
		public static final int VERY_UNSTABLE = 0x6;

		private Quality() { }
	}

	private static final String TAG = "NetworkEmulator";

	private int minLatency = 0;
	private int maxLatency = 0;

	private int avgStableTime = Integer.MAX_VALUE;
	private int stableTimeDeviation = 0;
	private long nextErrorTime = Integer.MAX_VALUE;

	private final Random random = new Random();

	public NetworkEmulator() {
		setNetworkQuality(Quality.IDEAL);
	}

	public void setNetworkQuality(int quality) {
		switch (quality) {
		case Quality.IDEAL:
			setLatencyRange(0, 0);
			setStableWorkTime(Integer.MAX_VALUE, 0);
			break;
		case Quality.LAN:
			setLatencyRange(0, 4);
			setStableWorkTime(5*60*60*1000, 60*60*1000);
			break;
		case Quality.CELL:
		case Quality.WIFI:
			setLatencyRange(50, 200);
			setStableWorkTime(20*60*1000, 10*60*1000);
			break;
		case Quality.GPRS:
			setLatencyRange(750, 2500);
			setStableWorkTime(60*1000, 50*1000);
			break;
		case Quality.VERY_UNSTABLE:
			setLatencyRange(250, 1000);
			setStableWorkTime(5*1000, 5*1000);
		}
	}

	public void setStableWorkTime(int avgStableTime, int stableTimeDeviation) {
		this.avgStableTime = avgStableTime;
		this.stableTimeDeviation = stableTimeDeviation;

		setUpNextError();
	}

	public void setLatencyRange(int minMillis, int maxMillis) {
		if ((minMillis < 0) || (maxMillis < 0) || (maxMillis < minMillis)) {
			throw new IllegalArgumentException("Invalid latency range");
		}
		minLatency = minMillis;
		maxLatency = maxMillis;
	}

	public void emulateNetworkOperation() throws IOException {		
		if (System.currentTimeMillis() >= nextErrorTime) {
			Log.debug(TAG, "Deciding to break connection");

			setUpNextError();

			throw new IOException("Network emulator decided to break connection");
		}

		final int thisOpLatency = minLatency + random.nextInt(maxLatency - minLatency + 1);

		Log.debug(TAG, "Network latency for this op is " + thisOpLatency + "ms");

		try {
			Thread.sleep(thisOpLatency);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	private void setUpNextError() {
		long nextErrorAfter = avgStableTime + random.nextInt(stableTimeDeviation*2 + 1) - stableTimeDeviation;

		nextErrorTime = System.currentTimeMillis() + nextErrorAfter;

		Log.debug(TAG, "Next error will occur after " + nextErrorAfter + "ms");
	}
}
