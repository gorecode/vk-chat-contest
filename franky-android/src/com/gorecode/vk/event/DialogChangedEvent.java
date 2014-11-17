package com.gorecode.vk.event;

import com.google.common.base.Preconditions;
import com.gorecode.vk.data.Dialog;

public class DialogChangedEvent {
	public final Dialog dialog;

	public DialogChangedEvent(Dialog dialog) {
		Preconditions.checkNotNull(dialog);

		this.dialog = dialog;
	}
}
