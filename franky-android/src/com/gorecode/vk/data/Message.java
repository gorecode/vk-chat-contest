package com.gorecode.vk.data;

import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.uva.lang.StringUtilities;

public class Message<C> implements Serializable {
	private static final long serialVersionUID = 7908010916681732041L;

	public static class Content implements Serializable {		
		private static final long serialVersionUID = 8122907536144979084L;
		
		public String text;		

		public Content() {
			;
		}

		public Content(String text) {
			Preconditions.checkNotNull(text);

			this.text = makeMessageSafe(text);
		}

		@Override
		public String toString() {
			return "ChatMessage.Content [message=" + text + "]";
		}

		public static String makeMessageSafe(String text) {
			return StringUtilities.trimRepeatingSymbols(text, "\n", 2);
		}

		public void setText(String text) {
			this.text = makeMessageSafe(text);
		}
		
		@Override
		public int hashCode() {
			final int prime = 21256813;
			int result = 1;
			result = prime * result + ((text == null) ? 0 : text.hashCode());
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
			Content other = (Content) obj;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			return true;
		}
	}

	public static final int DIRECTION_INCOMING = 0x0;
	public static final int DIRECTION_OUTGOING = 0x1;

	public long id;

	public int direction;

	public Profile user;

	public long timestamp;

	public C content;

	public boolean isOk() {
		return id != 0 && user != null && timestamp > 0;
	}

	public boolean isOutgoing() {
		return direction == DIRECTION_OUTGOING;
	}

	public boolean isIncoming() {
		return direction == DIRECTION_INCOMING;
	}

	public long getMid() {
		return id;
	}

	public Profile getSender() {
		return user;
	}

	public Profile getParticipant() {
		return user;
	}

	public void fillMessageInfoFrom(Message<?> source) {
		this.direction = source.direction;
		this.id = source.id;
		this.user = source.user;
		this.timestamp = source.timestamp;
	}
}
