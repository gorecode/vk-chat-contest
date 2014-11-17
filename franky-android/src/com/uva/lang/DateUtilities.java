package com.uva.lang;

import java.util.Calendar;
import java.util.Date;


public class DateUtilities {
	private static final Calendar calendar = Calendar.getInstance();

    /** The maximum date possible. */
    public static Date MAX_DATE = new Date(Long.MAX_VALUE);

	/**
     * <p>Checks if two dates are on the same day ignoring time.</p>
     * @param date1  the first date, not altered, not null
     * @param date2  the second date, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either date is <code>null</code>
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }
    
    /**
     * <p>Checks if two calendars represent the same day ignoring time.</p>
     * @param cal1  the first calendar, not altered, not null
     * @param cal2  the second calendar, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either calendar is <code>null</code>
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
    
    /**
     * <p>Checks if a date is today.</p>
     * @param date the date, not altered, not null.
     * @return true if the date is today.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isToday(Date date) {
        return isSameDay(date, Calendar.getInstance().getTime());
    }
    
    /**
     * <p>Checks if a calendar date is today.</p>
     * @param cal  the calendar, not altered, not null
     * @return true if cal date is today
     * @throws IllegalArgumentException if the calendar is <code>null</code>
     */
    public static boolean isToday(Calendar cal) {
        return isSameDay(cal, Calendar.getInstance());
    }
    
    /**
     * <p>Checks if the first date is before the second date ignoring time.</p>
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if the first date day is before the second date day.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isBeforeDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isBeforeDay(cal1, cal2);
    }
    
    /**
     * <p>Checks if the first calendar date is before the second calendar date ignoring time.</p>
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return true if cal1 date is before cal2 date ignoring time.
     * @throws IllegalArgumentException if either of the calendars are <code>null</code>
     */
    public static boolean isBeforeDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        if (cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) return true;
        if (cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) return false;
        if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return true;
        if (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) return false;
        return cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * <p>Checks if the first date is after the second date ignoring time.</p>
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if the first date day is after the second date day.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isAfterDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isAfterDay(cal1, cal2);
    }
    
    /**
     * <p>Checks if the first calendar date is after the second calendar date ignoring time.</p>
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return true if cal1 date is after cal2 date ignoring time.
     * @throws IllegalArgumentException if either of the calendars are <code>null</code>
     */
    public static boolean isAfterDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        if (cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) return false;
        if (cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) return true;
        if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return false;
        if (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) return true;
        return cal1.get(Calendar.DAY_OF_YEAR) > cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * <p>Checks if a date is after today and within a number of days in the future.</p>
     * @param date the date to check, not altered, not null.
     * @param days the number of days.
     * @return true if the date day is after today and within days in the future .
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isWithinDaysFuture(Date date, int days) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return isWithinDaysFuture(cal, days);
    }
    
    /**
     * <p>Checks if a calendar date is after today and within a number of days in the future.</p>
     * @param cal the calendar, not altered, not null
     * @param days the number of days.
     * @return true if the calendar date day is after today and within days in the future .
     * @throws IllegalArgumentException if the calendar is <code>null</code>
     */
    public static boolean isWithinDaysFuture(Calendar cal, int days) {
        if (cal == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar today = Calendar.getInstance();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.DAY_OF_YEAR, days);
        return (isAfterDay(cal, today) && ! isAfterDay(cal, future));
    }
    
    /** Returns the given date with the time set to the start of the day. */
    public static Date getStart(Date date) {
        return clearTime(date);
    }
    
    /** Returns the given date with the time values cleared. */
    public static Date clearTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }    

    /** Determines whether or not a date has any time values (hour, minute, 
     * seconds or millisecondsReturns the given date with the time values cleared. */

    /**
     * Determines whether or not a date has any time values.
     * @param date The date.
     * @return true iff the date is not null and any of the date's hour, minute,
     * seconds or millisecond values are greater than zero.
     */
    public static boolean hasTime(Date date) {
        if (date == null) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if (c.get(Calendar.HOUR_OF_DAY) > 0) {
            return true;
        }
        if (c.get(Calendar.MINUTE) > 0) {
            return true;
        }
        if (c.get(Calendar.SECOND) > 0) {
            return true;
        }
        if (c.get(Calendar.MILLISECOND) > 0) {
            return true;
        }
        return false;
    }

    /** Returns the given date with time set to the end of the day */
    public static Date getEnd(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /** 
     * Returns the maximum of two dates. A null date is treated as being less
     * than any non-null date. 
     */
    public static Date max(Date d1, Date d2) {
        if (d1 == null && d2 == null) return null;
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return (d1.after(d2)) ? d1 : d2;
    }
    
    /** 
     * Returns the minimum of two dates. A null date is treated as being greater
     * than any non-null date. 
     */
    public static Date min(Date d1, Date d2) {
        if (d1 == null && d2 == null) return null;
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return (d1.before(d2)) ? d1 : d2;
    }

	public static int ageFromBirthday(Date birthday) {
		return ageFromBirthday(birthday, SystemClock.getInstance());
	}

	public static int ageFromBirthday(Date birthday, Clock clock) {
		final Calendar calendar = Calendar.getInstance();

		calendar.setTime(new Date(clock.currentTimeMillis()));
		int currentYear = calendar.get(Calendar.YEAR);
		int currentMonth = calendar.get(Calendar.MONTH);
		int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

		calendar.setTime(birthday);
		int birthdayYear = calendar.get(Calendar.YEAR);
		int birthdayMonth = calendar.get(Calendar.MONTH);
		int birthdayDay = calendar.get(Calendar.DAY_OF_MONTH);

		int age = currentYear - birthdayYear;

		if ((birthdayMonth > currentMonth) || (birthdayMonth == currentMonth && birthdayDay > currentDay)) {
			age--;
		}

		return Math.max(0, age);
	}

	/**
	 * Formats date to string in "MM dd yyyy" format.
	 * @param date date to format.
	 * @return formatted date.
	 */
	public static String format(Date date) {
		int year, month, day;

		synchronized (calendar) {
			calendar.setTime(date);		
			month = calendar.get(Calendar.MONTH) + 1;
			day = calendar.get(Calendar.DAY_OF_MONTH);
			year = calendar.get(Calendar.YEAR);
		}

		return new StringBuffer(11)
		.append(month < 10 ? "0" + month : String.valueOf(month)).append(" ")
		.append(day < 10 ? "0" + day : String.valueOf(day)).append(" ")
		.append(year)
		.toString();
	}

	/**
	 * Parses given string to date.
	 * @param str date string in "MM dd yyyy" format
	 * @return parsed date.
	 */
	public static Date parse(String str) {
		int month = Integer.parseInt(str.substring(0, 2)) - 1;
		int day = Integer.parseInt(str.substring(3, 5));
		int year = Integer.parseInt(str.substring(6));

		synchronized (calendar) {
			calendar.set(Calendar.YEAR, year);
			calendar.set(Calendar.MONTH, month);
			calendar.set(Calendar.DAY_OF_MONTH, day);			
			return calendar.getTime();
		}
	}

	/**
	 * Checks whether given birthdays are equal. If two birthdays have equal year, month and day of month, but different hours, minutes 
	 * and seconds, these two birthdays are considered equal. Any of given birthdays can be null. */
	public static boolean areBirthdaysEqual(Date birthday1, Date birthday2) {
		if (ObjectUtilities.equals(birthday1, birthday2)) return true;
		if (birthday1 == null ^ birthday2 == null) return false;
		
		return (birthday1.getMonth() == birthday2.getMonth() && birthday1.getYear() == birthday2.getYear()
				&& birthday1.getDate() == birthday2.getDate());
	}
	
	private DateUtilities() {
		;
	}
}
