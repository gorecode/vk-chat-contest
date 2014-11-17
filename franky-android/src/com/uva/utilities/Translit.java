package com.uva.utilities;

import java.util.HashMap;

public class Translit {
	private static final String[] sRussianLowercase;
	private static final String[] sRussianUppercase;
	private static final String[] sEnglish;

	private static final HashMap<Character, String> sRussianToEnglish;

	public static String toEnglish(String string) {
		StringBuffer builder = new StringBuffer();

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);

			if (sRussianToEnglish.containsKey(c)) {
				builder.append(sRussianToEnglish.get(c));
			} else {
				builder.append(c);
			}
		}

		return builder.toString();
	}

	private Translit() {
		;
	}

	static {
		sRussianLowercase = new String[32];

		sRussianLowercase[0] = "�";
		sRussianLowercase[1] = "�";
		sRussianLowercase[2] = "�";
		sRussianLowercase[3] = "�";
		sRussianLowercase[4] = "�";
		sRussianLowercase[5] = "�";
		sRussianLowercase[6] = "�";
		sRussianLowercase[7] = "�";
		sRussianLowercase[8] = "�";
		sRussianLowercase[9] = "�";
		sRussianLowercase[10] = "�";
		sRussianLowercase[11] = "�";
		sRussianLowercase[12] = "�";
		sRussianLowercase[13] = "�";
		sRussianLowercase[14] = "�";
		sRussianLowercase[15] = "�";
		sRussianLowercase[16] = "�";
		sRussianLowercase[17] = "�";
		sRussianLowercase[18] = "�";
		sRussianLowercase[19] = "�";
		sRussianLowercase[20] = "�";
		sRussianLowercase[21] = "�";
		sRussianLowercase[22] = "�";
		sRussianLowercase[23] = "�";
		sRussianLowercase[24] = "�";
		sRussianLowercase[25] = "�";
		sRussianLowercase[26] = "�";
		sRussianLowercase[27] = "�";
		sRussianLowercase[28] = "�";
		sRussianLowercase[29] = "�";
		sRussianLowercase[30] = "�";
		sRussianLowercase[31] = "�";

		sRussianUppercase = new String[32];

		sRussianUppercase[0] = "�";
		sRussianUppercase[1] = "�";
		sRussianUppercase[2] = "�";
		sRussianUppercase[3] = "�";
		sRussianUppercase[4] = "�";
		sRussianUppercase[5] = "�";
		sRussianUppercase[6] = "�";
		sRussianUppercase[7] = "�";
		sRussianUppercase[8] = "�";
		sRussianUppercase[9] = "�";
		sRussianUppercase[10] = "�";
		sRussianUppercase[11] = "�";
		sRussianUppercase[12] = "�";
		sRussianUppercase[13] = "�";
		sRussianUppercase[14] = "�";
		sRussianUppercase[15] = "�";
		sRussianUppercase[16] = "�";
		sRussianUppercase[17] = "�";
		sRussianUppercase[18] = "�";
		sRussianUppercase[19] = "�";
		sRussianUppercase[20] = "�";
		sRussianUppercase[21] = "�";
		sRussianUppercase[22] = "�";
		sRussianUppercase[23] = "�";
		sRussianUppercase[24] = "�";
		sRussianUppercase[25] = "�";
		sRussianUppercase[26] = "�";
		sRussianUppercase[27] = "�";
		sRussianUppercase[28] = "�";
		sRussianUppercase[29] = "�";
		sRussianUppercase[30] = "�";
		sRussianUppercase[31] = "�";

		sEnglish = new String[32];
		sEnglish[0] = "sch";
		sEnglish[1] = "zh";
		sEnglish[2] = "ch";
		sEnglish[3] = "sh";
		sEnglish[4] = "yu";
		sEnglish[5] = "ya";
		sEnglish[6] = "a";
		sEnglish[7] = "b";
		sEnglish[8] = "v";
		sEnglish[9] = "g";
		sEnglish[10] = "d";
		sEnglish[11] = "e";
		sEnglish[12] = "z";
		sEnglish[13] = "i";
		sEnglish[14] = "j";
		sEnglish[15] = "k";
		sEnglish[16] = "l";
		sEnglish[17] = "m";
		sEnglish[18] = "n";
		sEnglish[19] = "o";
		sEnglish[20] = "p";
		sEnglish[21] = "r";
		sEnglish[22] = "s";
		sEnglish[23] = "t";
		sEnglish[24] = "u";
		sEnglish[25] = "f";
		sEnglish[26] = "h";
		sEnglish[27] = "c";
		sEnglish[28] = "";
		sEnglish[29] = "y";
		sEnglish[30] = "";
		sEnglish[31] = "e";

		sRussianToEnglish = new HashMap<Character, String>();

		for (int i = 0; i < sRussianLowercase.length; i++) {
			sRussianToEnglish.put(sRussianLowercase[i].charAt(0), sEnglish[i]);
			sRussianToEnglish.put(sRussianUppercase[i].charAt(0), sEnglish[i]);
		}
	}
}
