package com.gorecode.vk.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.perm.kate.api.VkUser;

/**
 * Contains information about profile (your and alien).
 * Can mutate depending on context (if u don't know what it means, you're in trouble).
 * @author enikey
 */
public class Profile implements Serializable, Cloneable {
	private static final long serialVersionUID = -2149548643332844348L;

	public long id;
	public String firstName;
	public String lastName;
	public long lastActivityTime;
	public String nick;
	public ImageUrls avatarUrls;
	public Availability availability;	
	public String homePhone;
	public String mobilePhone;
	public String fullname;
	public String email;

	public UnhandledNotifications notificationSummary = new UnhandledNotifications();

	public Profile() {
	}

	public boolean isOnline() {
		return availability == Availability.ONLINE;
	}

	public String getHomePhone() {
		return homePhone;
	}

	public String getCellPhone() {
		return mobilePhone;
	}

	public String getEmail() {
		return email;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public long getUid() {
		return id;
	}

	public static Profile empty(long uid) {
		Profile profile = new Profile();
		profile.id = uid;
		profile.firstName = String.valueOf(uid);
		return profile;
	}

	public static List<Profile> fromVkUsers(Collection<VkUser> vkUsers) {
		ArrayList<Profile> profiles = new ArrayList<Profile>(vkUsers.size());
		for (VkUser vkUser : vkUsers) {
			profiles.add(fromVkUser(vkUser));
		}
		return profiles;
	}

	public static Profile fromVkUser(VkUser vkUser) {
		Profile user = new Profile();
		user.id = vkUser.uid;
		user.availability = Boolean.TRUE.equals(vkUser.online) ? Availability.ONLINE : Availability.OFFLINE;
		user.firstName = vkUser.first_name;
		user.lastName = vkUser.last_name;
		user.nick = vkUser.nickname;

		if (vkUser.photo_big != null) {
			user.avatarUrls = new ImageUrls();
			user.avatarUrls.previewUrl = vkUser.photo_small;
			user.avatarUrls.fullsizeUrl = vkUser.photo_big;
		} else {
			user.avatarUrls = null;
		}
		
		if (vkUser.mobile_phone != null) {
			user.mobilePhone = vkUser.mobile_phone;
		}
		if (vkUser.home_phone != null) {
			user.homePhone = vkUser.home_phone;
		}
		user.updateFullname();
		return user;
	}

	public boolean isEmpty() {
		return (firstName == null && lastName == null) || (lastName == null && String.valueOf(id).equals(firstName));
	}

	public byte[] toBytes() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(this);
			oos.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Should never happen", e);
		}
	}

	public String getFullname() {
		if (fullname == null) {
			updateFullname();
		}
		return fullname;
	}

	@Override
	public Profile clone() {
		try {
			return (Profile)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateFullname() {
		if (Strings.isNullOrEmpty(firstName) ^ Strings.isNullOrEmpty(lastName)) {
			fullname = Objects.firstNonNull(firstName, lastName);
		} else {
			fullname = firstName + " " + lastName;
		}		
	}
}
