package com.uva.lang;

import java.util.Enumeration;
import java.util.Vector;

public class EnumerationUtilities {
	public static Vector toVector(Enumeration e) {
		Vector vector = new Vector();

		while (e.hasMoreElements()) {
			vector.addElement(e.nextElement());
		}

		return vector;
	}

	private EnumerationUtilities() {
		;
	}
}
