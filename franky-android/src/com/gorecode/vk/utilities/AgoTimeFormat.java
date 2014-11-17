package com.gorecode.vk.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.res.Resources;

import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.uva.lang.DateUtilities;

public class AgoTimeFormat implements TimeFormatter {
	public static final int FORMAT_SHORT = 0x0;
	public static final int FORMAT_FULL = 0x1;

	private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	private final Resources mResources;
	private int mFormat;

	@Inject
	public AgoTimeFormat(Resources resources) {
		mResources = resources;

		mFormat = FORMAT_FULL;
	}

	public void setFormat(int format) {
		mFormat = format;
	}

	public String formatDayOnly(Date date) {
		if (DateUtilities.isToday(date)) {
			return mResources.getString(R.string.timeFormat_today);
		}

		if (DateUtilities.isSameDay(date, new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))) {
			return mResources.getString(R.string.timeFormat_yesterday);
		}

		return DAY_FORMAT.format(date);
	}

	public String formatTimeOnly(Date date) {
		return TIME_FORMAT.format(date);
	}

	@Override
	public String format(long timeMillis) {
		Date date = new Date(timeMillis);

		if (mFormat == FORMAT_SHORT) {
			if (DateUtilities.isToday(date)) {
				return formatTimeOnly(date);
			} else {
				return formatDayOnly(date);
			}
		} else {
			String dayString = formatDayOnly(date);
			String timeString = formatTimeOnly(date);

			return String.format("%s %s %s", dayString, mResources.getString(R.string.timeFormat_at), timeString);
		}
	}
}
