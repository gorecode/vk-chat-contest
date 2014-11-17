package com.gorecode.vk.data;

import java.io.Serializable;

import com.perm.kate.api.VkPhoto;
import com.uva.utilities.AssertCompat;

public class ImageUrls implements Cloneable, Serializable {	
	private static final long serialVersionUID = -5100675968566615984L;
	
	public String previewUrl;
	public String fullsizeUrl;

	public ImageUrls(String previewUrl, String fullsizeUrl) {
		this.previewUrl = previewUrl;
		this.fullsizeUrl = fullsizeUrl;
	}

	public ImageUrls() {
		;
	}

	public static ImageUrls fromVkPhoto(VkPhoto vkPhoto) {
		return new ImageUrls(vkPhoto.src, vkPhoto.src_big);
	}

	public boolean isOk() {
		return previewUrl != null && fullsizeUrl != null;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			AssertCompat.shouldNeverHappen(e);
			return null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 189845707;
		int result = 1;
		result = prime * result
				+ ((fullsizeUrl == null) ? 0 : fullsizeUrl.hashCode());
		result = prime * result
				+ ((previewUrl == null) ? 0 : previewUrl.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageUrls other = (ImageUrls) obj;
		if (fullsizeUrl == null) {
			if (other.fullsizeUrl != null)
				return false;
		} else if (!fullsizeUrl.equals(other.fullsizeUrl))
			return false;
		if (previewUrl == null) {
			if (other.previewUrl != null)
				return false;
		} else if (!previewUrl.equals(other.previewUrl))
			return false;
		return true;
	}	
}
