package com.gorecode.vk.data;

import java.util.Comparator;

import com.gorecode.vk.collections.SortedTable;

public class UsersTable extends SortedTable<Profile> {
	private static final Comparator<Profile> BY_NAME = new Comparator<Profile>() {
		@Override
		public int compare(Profile object1, Profile object2) {
			String name1 = object1.getFullname();
			String name2 = object2.getFullname();
			return name1.compareTo(name2);
		}
	};

	public UsersTable() {
		super(BY_NAME);
	}

	@Override
	public long getIdOfObject(Profile object) {
		return object.id;
	}
}
