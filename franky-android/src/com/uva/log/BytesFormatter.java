package com.uva.log;

public class BytesFormatter {
	public final static int DEFAULT_MAX_COUNT_OF_BYTES_TO_PRINT = 1024;

	private final static String[] BYTE_TO_HEX_STRING_TABLE;

	static {
		// Optimization for deadly slow Android.
		BYTE_TO_HEX_STRING_TABLE = new String[256];
		for (int i = 0; i < BYTE_TO_HEX_STRING_TABLE.length; i++) {
			BYTE_TO_HEX_STRING_TABLE[i] = Integer.toHexString(i);
		}
	}

	public static String format(byte[] bytes) {
		return format(bytes, DEFAULT_MAX_COUNT_OF_BYTES_TO_PRINT);
	}

	/**
	 * Creates human readable string from binary dump.<br>
	 * String format: AA BB DD CC DD ...<br>
	 * If dump too long (more than 1024 bytes) rest of it will be skipped and "..." added to the end of result string.
	 * @param bytes the binary dump.
	 * @param maxBytesCountToPrint count of max bytes to print into output string.
	 * @return human readable binary dump string.
	 */
	public static String format(byte[] bytes, int maxBytesCountToPrint) {
		final boolean tooManyBytes = bytes.length > maxBytesCountToPrint;

		final int truncatedBytesLength = tooManyBytes ? maxBytesCountToPrint : bytes.length;

		final StringBuffer formatted = new StringBuffer(truncatedBytesLength * 5 + 3);

		for (int i = 0; i < truncatedBytesLength; i++) {
			formatted.append(BYTE_TO_HEX_STRING_TABLE[bytes[i] & 0xFF]);
			formatted.append(' ');
		}

		if (tooManyBytes) {
			formatted.append("...");
		}

		return formatted.toString();
	}

	private BytesFormatter() { }
}
