package com.gorecode.vk.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.uva.lang.StringUtilities;
import com.uva.utilities.Translit;

public class UserSearch {
	private static final Function<Profile, String> FUNCTION_GET_USER_NAME = new Function<Profile, String>() {
		@Override
		public String apply(Profile arg) {
			return arg.getFullname();
		}
	};

	public static List<Profile> filterByQuery(List<Profile> users, String query) {
		return filterByQuery(users, query, FUNCTION_GET_USER_NAME);
	}

	public static <U> List<U> filterByQuery(List<U> users, String query, Function<U, String> getNameFunc) {
		if (Strings.isNullOrEmpty(query)) {
			return users;
		} else {
			query = makeLocaleIndependent(query.replaceAll("\\s+", " ").trim());

			List<U> filtered = new ArrayList<U>();

			for (U user : users) {
				String fullname = makeLocaleIndependent(getNameFunc.apply(user).replaceAll("\\s+", " ").trim());

				boolean shouldSkip = true;

				if (firstOrLastNameStartsWith(fullname, query)) {
					shouldSkip = false;
				} else {
					String[] queryWords = StringUtilities.split(query, " ");

					if (queryWords.length > 1) {
						String q1 = queryWords[0];
						String q2 = queryWords[1];

						if (firstOrLastNameStartsWith(fullname, q1) && firstOrLastNameStartsWith(fullname, q2)) {
							shouldSkip = false;
						}
					}
				}

				if (shouldSkip) {
					continue;
				}

				filtered.add(user);
			}

			return filtered;
		}
	}

	private static boolean firstOrLastNameStartsWith(String fullname, String prefix) {
		final int indexOfPrefix = fullname.indexOf(prefix);

		return (indexOfPrefix >= 0 && (indexOfPrefix == 0 || fullname.charAt(indexOfPrefix - 1) == ' '));
	}

	private static String makeLocaleIndependent(String query) {
		return Translit.toEnglish(query.toLowerCase(Locale.US)).trim();
	}

	private UserSearch() {
		;
	}
}
