package com.gorecode.vk.event;

import com.gorecode.vk.data.Dialog;

public class DialogDeletedEvent {
	public final Dialog dialog;

	public DialogDeletedEvent(Dialog dialog) {
		this.dialog = dialog;
	}
}
