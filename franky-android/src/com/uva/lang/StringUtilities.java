package com.uva.lang;

import java.util.Arrays;
import java.util.List;

public class StringUtilities {
	public static boolean isEmpty(String string) {
		return (string == null || string.length() == 0);
	}

	public static String join(String delimiter, String... strings) {
		return join(delimiter, Arrays.asList(strings));
	}

	public static String join(String delimiter, List<String> strings) {
		int high = strings.size() - 1;
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < high; i++) {
			out.append(strings.get(i)).append(delimiter);
		}
		if (high >= 0) {
			out.append(strings.get(high));
		}
		return out.toString();
	}

	public static String trimRepeatingSymbols(String source, String charsToTrim, int maxAcceptableRepeatCount) {
		if (source == null) {
			return null;
		}
		
		StringBuffer buffer = new StringBuffer(source.length());

		int currentRepeatCount = 0;

		Character previousCharacter = null;

		for (int i = 0; i < source.length(); i++) {
			char currentCharacter = (source.charAt(i));

			if (charsToTrim.indexOf(currentCharacter) >= 0 && ObjectUtilities.equals(previousCharacter, currentCharacter)) {
				if (++currentRepeatCount >= maxAcceptableRepeatCount) {
					continue;
				}
			} else {
				currentRepeatCount = 0;
			}

			buffer.append(currentCharacter);

			previousCharacter = currentCharacter;
		}

		return buffer.toString();
	}

	public static String[] split(String src, String delim) {
		StringTokenizer tok = new StringTokenizer(src, delim);
		String[] pieces = new String[tok.countTokens()];
		int i = 0;
		while (tok.hasMoreTokens()) {
			pieces[i++] = tok.nextToken();
		}
		return pieces;
	}

	public static boolean equalsIgnoreCase(String s1, String s2) {
		if ((s1 == null) && (s2 == null)) {
			return true;
		}
		if ((s1 == null) ^ (s2 == null)) {
			return false;
		}
		if (s1.length() != s2.length()) {
			return false;
		}

		int length = s1.length();

		for (int i = 0; i < length; i++) {
			char c1 = s1.charAt(i);
			char c2 = s2.charAt(i);

			if (Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
				return false;
			}
		}

		return true;
	}

	public static String toLower(String source) {
		if (source == null) {
			return null;
		}

		final int length = source.length();

		StringBuffer buffer = new StringBuffer(length);

		for (int i = 0; i < length; i++) {
			char lowerChar = Character.toLowerCase(source.charAt(i));

			buffer.append(lowerChar);
		}

		return buffer.toString();
	}

	public static String replace(String source, String pattern, String replacement) {
		StringBuffer sb = new StringBuffer();
		int idx;
		int patIdx = 0;

		while ((idx = source.indexOf(pattern, patIdx)) != -1) {
			sb.append(source.substring(patIdx, idx));
			sb.append(replacement);
			patIdx = idx + pattern.length();
		}
		sb.append(source.substring(patIdx));
		return sb.toString();
	}

	private StringUtilities() {
		;
	}
}
