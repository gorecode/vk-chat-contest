package com.gorecode.vk.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import com.uva.location.Location;

public class FastPackUnpack {
	public static void writeDocumentAttachment(DataOutputStream out, Document value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeLong(value.documentId);
		out.writeLong(value.ownerId);
		out.writeInt(value.size);
		writeString(out, value.title);
		writeString(out, value.url);
		writeString(out, value.extension);
	}

	public static void writeDocumentsAttachments(DataOutputStream out, Collection<Document> values) throws IOException {
		out.writeInt(values.size());
		for (Document value : values) {
			writeDocumentAttachment(out, value);
		}
	}

	public static Document readDocumentAttachment(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Document value = new Document();
		value.documentId = in.readLong();
		value.ownerId = in.readLong();
		value.size = in.readInt();
		value.title = readString(in);
		value.url = readString(in);
		value.extension = readString(in);
		return value;
	}

	public static ArrayList<Document> readDocumentsAttachments(DataInputStream in) throws IOException {
		final int size = in.readInt();
		ArrayList<Document> values = new ArrayList<Document>(size);
		for (int i = 0; i < size; i++) {
			values.add(readDocumentAttachment(in));
		}
		return values;
	}

	public static Dialog deserializeDialog(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readDialog(in);
		} finally {
			in.close();
		}
	}

	public static byte[] serializeDialog(Dialog value) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeDialog(new DataOutputStream(out), value);
		return out.toByteArray();
	}

	public static void writeDialog(DataOutputStream out, Dialog value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		writeChatMessage(out, value.lastMessage);
		out.writeInt(value.activeUsers.size());
		for (Profile participant : value.activeUsers) {
			writeProfile(out, participant);
		}
		out.writeInt(value.totalParticipants);
		out.writeLong(value.ownerId);
	}

	public static Dialog readDialog(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Dialog value = new Dialog();
		value.lastMessage = readChatMessage(in);
		final int count = in.readInt();
		for (int i = 0; i < count; i++) {
			Profile participant = readProfile(in);
			value.putActiveParticipant(participant);
		}
		value.totalParticipants = in.readInt();
		value.ownerId = in.readLong();
		return value;
	}

	public static void writeMessageContent(DataOutputStream out, Message.Content value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		writeString(out, value.text);
	}
	
	public static Message.Content readMessageContent(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Message.Content res = new Message.Content();
		res.text = readString(in);
		return res;
	}

	public static <T extends Enum<?>> T readEnum(DataInputStream in, T[] values) throws IOException {
		if (!in.readBoolean()) return null;

		return values[in.readInt()];
	}

	public static <T extends Enum<?>> void writeEnum(DataOutputStream out, T value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeInt(value.ordinal());
	}
	
	public static void writeLocation(DataOutputStream out, Location value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeDouble(value.latitude);
		out.writeDouble(value.longitude);
		out.writeDouble(value.accuracy);
		out.writeLong(value.timestamp);
	}
	
	public static Location readLocation(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Location res = new Location();
		res.latitude = in.readDouble();
		res.longitude = in.readDouble();
		res.accuracy = in.readDouble();
		res.timestamp = in.readLong();
		return res;
	}
	
	public static byte[] serializeLocation(Location value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeLocation(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Location deserializeLocation(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readLocation(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeProfiles(DataOutputStream out, Collection<Profile> values) throws IOException {
		out.writeInt(values.size());

		for (Profile value : values) {
			writeProfile(out, value);
		}
	}

	public static void writeProfile(DataOutputStream out, Profile value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		writeLong(out, value.id);
		writeString(out, value.firstName);
		writeString(out, value.lastName);
		writeString(out, value.nick);
		writeImageUrls(out, value.avatarUrls);
		writeAvailability(out, value.availability);
		writeString(out, value.mobilePhone);
		writeString(out, value.homePhone);
	}
	
	public static Profile readProfile(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Profile res = new Profile();
		res.id = readLong(in);
		res.firstName = readString(in);
		res.lastName = readString(in);
		res.nick = readString(in);
		res.avatarUrls = readImageUrls(in);
		res.availability = readAvailability(in);
		res.mobilePhone = readString(in);
		res.homePhone = readString(in);
		res.updateFullname();
		return res;
	}
	
	public static byte[] serializeProfile(Profile value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeProfile(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Profile deserializeProfile(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readProfile(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeLong(DataOutputStream out, Long value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeLong(value.longValue());
	}
	
	public static Long readLong(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		return new Long(in.readLong());
	}
	
	public static byte[] serializeLong(Long value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeLong(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Long deserializeLong(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readLong(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeDate(DataOutputStream out, Date value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeLong(value.getTime());
	}
	
	public static Date readDate(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		return new Date(in.readLong());
	}
	public static byte[] serializeDate(Date value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeDate(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Date deserializeDate(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readDate(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeInteger(DataOutputStream out, Integer value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeInt(value.intValue());
	}
	
	public static Integer readInteger(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		return new Integer(in.readInt());
	}
	
	public static byte[] serializeInteger(Integer value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeInteger(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Integer deserializeInteger(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readInteger(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeString(DataOutputStream out, String value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeUTF(value);
	}
	
	public static String readString(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		return in.readUTF();
	}
	
	public static byte[] serializeString(String value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeString(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static String deserializeString(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readString(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeUnhandledNotifications(DataOutputStream out, UnhandledNotifications value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeInt(value.numOffers);
		out.writeInt(value.numMessages);
	}
	
	public static UnhandledNotifications readUnhandledNotifications(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		UnhandledNotifications res = new UnhandledNotifications();
		res.numOffers = in.readInt();
		res.numMessages = in.readInt();
		return res;
	}
	
	public static byte[] serializeUnhandledNotifications(UnhandledNotifications value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeUnhandledNotifications(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static UnhandledNotifications deserializeUnhandledNotifications(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readUnhandledNotifications(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeGender(DataOutputStream out, Gender value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		writeString(out, value.toString());
	}
	
	public static Gender readGender(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		return Gender.fromString(readString(in));
	}
	
	public static byte[] serializeGender(Gender value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeGender(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Gender deserializeGender(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readGender(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeImageUrls(DataOutputStream out, ImageUrls value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		writeString(out, value.previewUrl);
		writeString(out, value.fullsizeUrl);
	}
	
	public static ImageUrls[] readImageUrlsArray(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return new ImageUrls[0];
		final int size = in.readInt();
		ImageUrls[] result = new ImageUrls[size];
		for (int i = 0; i < size; i++) {
			result[i] = readImageUrls(in);
		}
		return result;
	}

	public static ImageUrls readImageUrls(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		ImageUrls res = new ImageUrls();
		res.previewUrl = readString(in);
		res.fullsizeUrl = readString(in);
		return res;
	}
	
	public static byte[] serializeImageUrls(ImageUrls value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeImageUrls(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static ImageUrls deserializeImageUrls(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readImageUrls(in);
		} finally {
			in.close();
		}
	}
	
	public static void writebytes(DataOutputStream out, byte[] values) throws IOException {
		out.writeBoolean(values != null);
		if (values == null) return;
		
		out.writeInt(values.length);
		for (int i = 0; i < values.length; i++) {
			out.writeByte(values[i]);
		}
	}
	
	public static byte[] readbytes(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		int count = in.readInt();
		byte[] res = new byte[count];
		for (int i = 0; i < count; i++) {
			res[i] = in.readByte();
		}
		return res;
	}
	
	public static byte[] serializebytes(byte[] value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writebytes(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static byte[] deserializebytes(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readbytes(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeAvailability(DataOutputStream out, Availability value) throws IOException {
		writeEnum(out, value);
	}
	
	public static Availability readAvailability(DataInputStream in) throws IOException {
		return readEnum(in, Availability.values());
	}
	
	public static byte[] serializeAvailability(Availability value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeAvailability(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Availability deserializeAvailability(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readAvailability(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeBoolean(DataOutputStream out, Boolean value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeBoolean(value.booleanValue());
	}
	
	public static Boolean readBoolean(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		return new Boolean(in.readBoolean());
	}
	
	public static byte[] serializeBoolean(Boolean value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeBoolean(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Boolean deserializeBoolean(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readBoolean(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeDouble(DataOutputStream out, Double value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeDouble(value.doubleValue());
	}
	
	public static Double readDouble(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		return new Double(in.readDouble());
	}
	
	public static byte[] serializeDouble(Double value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeDouble(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Double deserializeDouble(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readDouble(in);
		} finally {
			in.close();
		}
	}
	
	public static void writeChatMessages(DataOutputStream out, Collection<ChatMessage> values) throws IOException {
		out.writeInt(values.size());
		for (ChatMessage value : values) {
			writeChatMessage(out, value);
		}
	}

	public static void writeChatMessage(DataOutputStream out, ChatMessage value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeBoolean(value.unread);
		out.writeLong(value.id);
		out.writeLong(value.chatId);
		out.writeInt(value.direction);
		writeProfile(out, value.user);
		out.writeLong(value.timestamp);
		writeChatMessageContent(out, value.content);
	}

	public static void writeAudios(DataOutputStream out, Collection<Audio> value) throws IOException {
		out.writeInt(value.size());
		for (Audio each : value) {
			writeAudio(out, each);
		}
	}

	public static void writeAudio(DataOutputStream out, Audio value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeLong(value.aid);
		writeString(out, value.artist);
		out.writeLong(value.duration);
		writeLong(out, value.lyricsId);
		out.writeLong(value.ownerId);
		writeString(out, value.title);
		writeString(out, value.url);
	}

	public static ArrayList<Audio> readAudios(DataInputStream in) throws IOException {
		int size = in.readInt();
		ArrayList<Audio> value = new ArrayList<Audio>(size);
		for (int i = 0; i < size; i++) {
			value.add(readAudio(in));
		}
		return value;
	}

	public static Audio readAudio(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Audio value = new Audio();
		value.aid = in.readLong();
		value.artist = readString(in);
		value.duration = in.readLong();
		value.lyricsId = readLong(in);
		value.ownerId = in.readLong();
		value.title = readString(in);
		value.url = readString(in);
		return value;
	}

	public static void writeChatMessageContent(DataOutputStream out, ChatMessage.Content value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		writeImageUrlsArray(out, value.imageUrls);
		writeLocation(out, value.location);
		writeString(out, value.text);
		writeString(out, value.subject);
		writeAudios(out, value.audios);
		writeDocumentsAttachments(out, value.documents);
		writeVideos(out, value.videos);
		writeChatMessages(out, value.forwarded);
	}

	public static void writeImageUrlsArray(DataOutputStream out, ArrayList<ImageUrls> array) throws IOException {
		out.writeBoolean(array != null);
		if (array == null) return;
		out.writeInt(array.size());
		for (ImageUrls each : array) {
			writeImageUrls(out, each);
		}
	}

	public static ArrayList<ChatMessage> readChatMessages(DataInputStream in) throws IOException {
		final int size = in.readInt();
		ArrayList<ChatMessage> values = new ArrayList<ChatMessage>();
		for (int i = 0; i < size; i++) {
			values.add(readChatMessage(in));
		}
		return values;
	}

	public static ChatMessage readChatMessage(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		ChatMessage res = new ChatMessage();
		res.unread = in.readBoolean();
		res.id = in.readLong();
		res.chatId = in.readLong();
		res.direction = in.readInt();
		res.user = readProfile(in);
		res.timestamp = in.readLong();
		res.content = readChatMessageContent(in);
		return res;
	}
	
	public static ChatMessage.Content readChatMessageContent(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		ChatMessage.Content res = new ChatMessage.Content();
		res.imageUrls.addAll(Arrays.asList(readImageUrlsArray(in)));
		res.location = readLocation(in);
		res.text = readString(in);
		res.subject = readString(in);
		res.audios = readAudios(in);
		res.documents = readDocumentsAttachments(in);
		res.videos = readVideos(in);
		res.forwarded = readChatMessages(in);
		return res;
	}

	public static void writeVideo(DataOutputStream out, Video value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeLong(value.date);
		writeString(out, value.description);
		out.writeLong(value.duration);
		writeString(out, value.image);
		writeString(out, value.link);
		out.writeLong(value.ownerId);
		writeString(out, value.player);
		writeString(out, value.title);
		out.writeLong(value.vid);
	}

	public static void writeVideos(DataOutputStream out, Collection<Video> values) throws IOException {
		out.writeInt(values.size());
		for (Video value : values) {
			writeVideo(out, value);
		}
	}

	public static Video readVideo(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Video value = new Video();
		value.date = in.readLong();
		value.description = readString(in);
		value.duration = in.readLong();
		value.image = readString(in);
		value.link = readString(in);
		value.ownerId = in.readLong();
		value.player = readString(in);
		value.title = readString(in);
		value.vid = in.readLong();
		return value;
	}

	public static ArrayList<Video> readVideos(DataInputStream in) throws IOException {
		final int size = in.readInt();
		ArrayList<Video> values = new ArrayList<Video>();
		for (int i = 0; i < size; i++) {
			values.add(readVideo(in));
		}
		return values;
	}

	public static byte[] serializeChatMessage(ChatMessage value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writeChatMessage(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static ChatMessage deserializeChatMessage(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readChatMessage(in);
		} finally {
			in.close();
		}
	}
	
	public static void writePhoto(DataOutputStream out, Photo value) throws IOException {
		out.writeBoolean(value != null);
		if (value == null) return;
		out.writeLong(value.id);
		writeString(out, value.description);
		writeImageUrls(out, value.imageUrls);
		out.writeInt(value.commentCount);
		out.writeInt(value.likeCount);
		out.writeLong(value.timestamp);
		out.writeBoolean(value.iLikeIt);
	}
	
	public static Photo readPhoto(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		Photo res = new Photo();
		res.id = in.readLong();
		res.description = readString(in);
		res.imageUrls = readImageUrls(in);
		res.commentCount = in.readInt();
		res.likeCount = in.readInt();
		res.timestamp = in.readLong();
		res.iLikeIt = in.readBoolean();
		return res;
	}
	
	public static byte[] serializePhoto(Photo value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writePhoto(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Photo deserializePhoto(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readPhoto(in);
		} finally {
			in.close();
		}
	}
	
	public static void writePhotos(DataOutputStream out, Photo[] values) throws IOException {
		out.writeBoolean(values != null);
		if (values == null) return;
		
		out.writeInt(values.length);
		for (int i = 0; i < values.length; i++) {
			writePhoto(out, values[i]);
		}
	}
	
	public static Photo[] readPhotos(DataInputStream in) throws IOException {
		if (!in.readBoolean()) return null;
		int count = in.readInt();
		Photo[] res = new Photo[count];
		for (int i = 0; i < count; i++) {
			res[i] = readPhoto(in);
		}
		return res;
	}
	
	public static byte[] serializePhotos(Photo[] value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		try {
			writePhotos(out, value);
			return bytes.toByteArray();
		} finally {
			out.close();
		}
	}
	
	public static Photo[] deserializePhotos(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			return readPhotos(in);
		} finally {
			in.close();
		}
	}
	
}

