package com.uva.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjectUtilities {
	/**
	 * Checks whether given objects are equal. Any of given objects can be 
	 * null.
	 * @param o1 first object.
	 * @param o2 second object.
	 * @return true if objects are equal or both is null and false, otherwise.
	 */
	public static boolean equals(Object o1, Object o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null ^ o2 == null) return false;

		return o1.equals(o2);
	}

	@SuppressWarnings("unchecked")
	public static <T /*extends Serializable*/> T fromBytes(byte[] bytes) throws Exception {
		return (T)new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
	}

	public static byte[] toBytes(Serializable object) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(object);
			oos.flush();
		} catch (IOException e) {
			throw new RuntimeException("Could not serialize object", e);
		}
		return bos.toByteArray();
	}

	public static boolean isNullOrEmpty(Object[] objects) {
		return objects == null || objects.length == 0;
	}
	
	public static <T> T firstNonNull(T... objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] != null) return objects[i];
		}
		return null;
	}
}
