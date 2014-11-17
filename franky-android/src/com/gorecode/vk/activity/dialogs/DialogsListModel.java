package com.gorecode.vk.activity.dialogs;

import java.util.Comparator;

import com.gorecode.vk.collections.Table;
import com.gorecode.vk.collections.TableChange;
import com.gorecode.vk.collections.TableChanges;
import com.gorecode.vk.collections.TableReflection;
import com.gorecode.vk.data.Dialog;

public class DialogsListModel extends TableReflection<Dialog, DialogListItem> {
	private static final Comparator<DialogListItem> FROM_NEWEST_TO_OLDEST = new Comparator<DialogListItem>() {
		@Override
		public int compare(DialogListItem n1, DialogListItem n2) {
			return Long.signum(n2.dialog.lastMessage.timestamp - n1.dialog.lastMessage.timestamp);
		}		
	};

	public DialogsListModel(Table<Dialog> source) {
		super(source, FROM_NEWEST_TO_OLDEST);
	}

	@Override
	public long getIdOfObject(DialogListItem object) {
		return object.dialog.getCid();
	}

	@Override
	protected void reflectChanges(TableChanges<Dialog> changes) {
		for (TableChange<Dialog> change : changes) {
			if (change.isValueDeleted()) {
				removeById(change.getValue().getCid());
			}
			if (change.isValuePut()) {
				put(DialogListItem.forDialog(change.getValue()));
			}
		}
		notifyTableChanged();
	}
}
