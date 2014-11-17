package com.gorecode.vk.data;

import java.io.Serializable;

public class UnhandledNotifications implements Serializable, Cloneable {
	private static final long serialVersionUID = -8539911299786650497L;

	public int numOffers;
	public int numMessages;

	public UnhandledNotifications() {
		;
	}

	public UnhandledNotifications(UnhandledNotifications source) {
		numOffers = source.numOffers;
		numMessages = source.numMessages;
	}

	public static UnhandledNotifications add(UnhandledNotifications first, UnhandledNotifications second) {
		UnhandledNotifications result = new UnhandledNotifications();
		result.numOffers = first.numOffers + second.numOffers;
		result.numMessages = first.numMessages + second.numMessages;
		return result;
	}

	public int summaryCount() {
		return numOffers + numMessages;
	}

	@Override
	public UnhandledNotifications clone() {
		try {
			return (UnhandledNotifications)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
