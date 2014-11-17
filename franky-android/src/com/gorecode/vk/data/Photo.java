package com.gorecode.vk.data;

import java.io.Serializable;

import com.uva.utilities.AssertCompat;

public class Photo implements Cloneable, Serializable {
	private static final long serialVersionUID = -6733133833078596933L;

	public long id;
	public String description;
	public ImageUrls imageUrls;
	public int commentCount;
	public int likeCount;
	public long timestamp;
	public boolean iLikeIt;

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			AssertCompat.shouldNeverHappen(e);
			return null;
		}
	}
}
