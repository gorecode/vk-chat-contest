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

		sRussianLowercase[0] = "ù";
		sRussianLowercase[1] = "æ";
		sRussianLowercase[2] = "÷";
		sRussianLowercase[3] = "ø";
		sRussianLowercase[4] = "þ";
		sRussianLowercase[5] = "ÿ";
		sRussianLowercase[6] = "à";
		sRussianLowercase[7] = "á";
		sRussianLowercase[8] = "â";
		sRussianLowercase[9] = "ã";
		sRussianLowercase[10] = "ä";
		sRussianLowercase[11] = "å";
		sRussianLowercase[12] = "ç";
		sRussianLowercase[13] = "è";
		sRussianLowercase[14] = "é";
		sRussianLowercase[15] = "ê";
		sRussianLowercase[16] = "ë";
		sRussianLowercase[17] = "ì";
		sRussianLowercase[18] = "í";
		sRussianLowercase[19] = "î";
		sRussianLowercase[20] = "ï";
		sRussianLowercase[21] = "ð";
		sRussianLowercase[22] = "ñ";
		sRussianLowercase[23] = "ò";
		sRussianLowercase[24] = "ó";
		sRussianLowercase[25] = "ô";
		sRussianLowercase[26] = "õ";
		sRussianLowercase[27] = "ö";
		sRussianLowercase[28] = "ú";
		sRussianLowercase[29] = "û";
		sRussianLowercase[30] = "ü";
		sRussianLowercase[31] = "ý";

		sRussianUppercase = new String[32];

		sRussianUppercase[0] = "Ù";
		sRussianUppercase[1] = "Æ";
		sRussianUppercase[2] = "×";
		sRussianUppercase[3] = "Ø";
		sRussianUppercase[4] = "Þ";
		sRussianUppercase[5] = "ß";
		sRussianUppercase[6] = "À";
		sRussianUppercase[7] = "Á";
		sRussianUppercase[8] = "Â";
		sRussianUppercase[9] = "Ã";
		sRussianUppercase[10] = "Ä";
		sRussianUppercase[11] = "Å";
		sRussianUppercase[12] = "Ç";
		sRussianUppercase[13] = "È";
		sRussianUppercase[14] = "É";
		sRussianUppercase[15] = "Ê";
		sRussianUppercase[16] = "Ë";
		sRussianUppercase[17] = "Ì";
		sRussianUppercase[18] = "Í";
		sRussianUppercase[19] = "Î";
		sRussianUppercase[20] = "Ï";
		sRussianUppercase[21] = "Ð";
		sRussianUppercase[22] = "Ñ";
		sRussianUppercase[23] = "Ò";
		sRussianUppercase[24] = "Ó";
		sRussianUppercase[25] = "Ô";
		sRussianUppercase[26] = "Õ";
		sRussianUppercase[27] = "Ö";
		sRussianUppercase[28] = "Ú";
		sRussianUppercase[29] = "Û";
		sRussianUppercase[30] = "Ü";
		sRussianUppercase[31] = "Ý";

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
