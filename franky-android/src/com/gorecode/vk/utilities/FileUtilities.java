package com.gorecode.vk.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.uva.log.Log;

public class FileUtilities {
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final String TAG = "FileUtilities";

	public static File fromUri(Context context, Uri uri) {
		if (uri.getScheme().equals("content")) {
			return fromContentUri(context, uri);
		}
		return fromFileUri(uri);
	}

	public static File fromFileUri(Uri fileUri) {
		try {
			return new File(new URI(fileUri.toString()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}

	public static File fromContentUri(Context context, Uri contentUri) {
		String[] projection = { MediaStore.Images.Media.DATA };

		Cursor cursor = context.getApplicationContext().getContentResolver().query(contentUri, projection, null, null, null);

		cursor.moveToFirst();

		try {
			return new File(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));
		} finally {
			cursor.close();
		}
	}

	public static boolean copyFileToFile(Context context, File src, File dst) {
		if (context != null && src != null && dst != null) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(src), BUFFER_SIZE);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst), BUFFER_SIZE);

				// Transfer bytes from in to out
				byte[] buf = new byte[BUFFER_SIZE];
				int len;

				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				in.close();
				out.close();
				return true;
			} catch (Exception e) {
				Log.exception(TAG, "unable to copy file " + src.getPath() + " to file " + dst.getPath(), e);
			}
		}
		
		return false;
	}
	
	public static boolean moveFileToFile(Context context, File src, File dst) {
		if (context != null && src != null && dst != null) {
			File extDir = Environment.getExternalStorageDirectory();
			File intDir = context.getFilesDir();

			// If src and dst are on the same filesystem, just renameTo()
			if ((src.getPath().startsWith(extDir.getPath()) && dst.getPath().startsWith(extDir.getPath())) ||
				(src.getPath().startsWith(intDir.getPath()) && dst.getPath().startsWith(intDir.getPath()))) {
				return src.renameTo(dst);
			}
			
			// Otherwise, copy and delete src
			if (copyFileToFile(context, src, dst)) {
				return src.delete();
			}
		}
		
		return false;
	}

	public static boolean copyUriToFile(Context context, Uri uri, File dst) {
		if (context != null && uri != null && dst != null) {
			try {
				BufferedInputStream in = new BufferedInputStream(context.getContentResolver().openInputStream(uri), BUFFER_SIZE);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst), BUFFER_SIZE);
				// Transfer bytes from in to out
				byte[] buf = new byte[BUFFER_SIZE];
				int len;

				while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
				}

				in.close();
				out.close();
				return true;
			} catch (Exception e) {
				Log.exception(TAG, "unable to copy URI " + uri.toString() + " to file " + dst.getPath(), e);
			}
		}
		
		return false;
	}
	
	public static File createUniqueFile(File directory, String filename) {
		File file = new File(directory, filename);
		
		if (file != null && file.exists()) {
			// There is already a file here with the desired name, so let's
			// loop and create a unique one with a numeric suffix.
			// 
			// This is awful, find a library that does this for us.
			try {
				int index = 0;
				
				while (file.exists() && index <= 128) {
					file = new File(directory, "[" + (++index) + "]" + filename);
				}
				
				if (file.exists()) {
					file = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				file = null;
			}
		}
		
		return file;
	}

	private FileUtilities() {
		;
	}
}
