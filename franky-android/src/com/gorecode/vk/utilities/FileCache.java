package com.gorecode.vk.utilities;

import java.io.File;
import java.io.IOException;

import com.uva.log.Log;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateUtils;

public class FileCache {
	private static final String NOMEDIA_FILENAME = ".nomedia";
	private static final long CACHE_FILE_EXPIRATION = DateUtils.DAY_IN_MILLIS * 4;
	private static final String LOGCAT_NAME = "FileUtil";

	private boolean autoCleanEnabled = true;
	private final Context context;

	public FileCache(Context context) {
		this.context = context;
	}

	public void setAutoCleanEnabled(boolean enabled) {
		autoCleanEnabled = enabled;
	}

	public boolean isAutoCleanEnabled() {
		return autoCleanEnabled;
	}

	public File addFileToCacheOrThrow(String fileName) throws IOException {
		File file = addFileToCache(fileName);
		if (file == null) {
			throw new IOException("Cannot add file with fileName = " + fileName + " to cache");
		}
		return file;
	}

	public File addFileToCache(String fileName) {
		if (autoCleanEnabled) cleanCaches();

		File file = addFileToExternalCache(context, fileName);
		if (file == null) {
			file = addFileToCache(fileName);
		}
		return file;
	}

	public File getFileFromCache(String fileName) {
		File file = getFileFromExternalCache(context, fileName);
		if (file == null) {
			file = getFileFromCache(context, fileName);
		}
		return file;
	}

	public void cleanCaches() {
		cleanCaches(context);
	}

	public static File getExternalCacheDir(Context context) {
		return getExternalStorageDir(context, "/cache");
	}

	public static File getInternalCacheDir(Context context) {
		if (context != null) {
			File intCacheDir = new File(context.getCacheDir(), "cache");
			if (!intCacheDir.exists()) {
				intCacheDir.mkdirs();
			}

			return intCacheDir;
		}
		return null;
	}

	public static File addFileToCache(Context context, String fileName) {
		File intCacheDir = getInternalCacheDir(context);
		return addFileToCache(context, fileName, intCacheDir);
	}

	public static File addFileToExternalCache(Context context, String fileName) {
		File extCacheDir = getExternalCacheDir(context);
		return addFileToCache(context, fileName, extCacheDir);
	}

	public static File addFileToCache(Context context, String fileName, File cacheDir) {
		if (context != null) {
			if (cacheDir != null) {
				File cachedFile = new File(cacheDir, fileName);
				if(!cachedFile.exists()){
					try{
						cachedFile.createNewFile();
					} catch (IOException e) {
						Log.exception(LOGCAT_NAME, "unable to create file in " + cachedFile.getPath(), e);
					}
				}
				return cachedFile;
			}
		}
		return null;
	}

	public static File getFileFromCache(Context context, String fileName){
		File intCacheDir = getInternalCacheDir(context);
		return getFileFromCache(context, fileName, intCacheDir);
	}

	public static File getFileFromExternalCache(Context context, String fileName){
		File extCacheDir = getInternalCacheDir(context);
		return getFileFromCache(context, fileName, extCacheDir);
	}

	public static File getFileFromCache(Context context, String fileName, File cacheDir){
		if (context != null) {
			if (cacheDir != null ) {
				File cachedFile = new File(cacheDir, fileName);
				if(cachedFile.exists()){
					return cachedFile;
				}
			} 
		}
		return null;
	}

	public static void cleanCaches(Context context) {
		if (context != null) {
			Log.message(LOGCAT_NAME, "cleaning up caches");
			File internalDir = getInternalCacheDir(context);
			
			if (internalDir != null) {
				File internalFiles[] = internalDir.listFiles();
		
				if (internalFiles != null && internalFiles.length > 0) {
					for (File file : internalFiles) {
						if (System.currentTimeMillis() - file.lastModified() >= CACHE_FILE_EXPIRATION) {
							Log.debug(LOGCAT_NAME, "deleting " + file.getPath());
							file.delete();
						}
					}
				}
			}
	
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				File externalDir = getExternalCacheDir(context);
				
				if (externalDir != null) {
					File[] externalFiles = externalDir.listFiles();
					
					if (externalFiles != null && externalFiles.length > 0) {
						for (File file : externalFiles) {
							if (System.currentTimeMillis() - file.lastModified() >= CACHE_FILE_EXPIRATION && !NOMEDIA_FILENAME.equals(file.getName())) {
								Log.debug(LOGCAT_NAME, "deleting " + file.getPath());
								file.delete();
							}
						}
					}
				}
			}
		}
	}

	private static File getExternalStorageDir(Context context, String dir) {
		if (context != null && dir != null) {
			File extMediaDir = new File(
				Environment.getExternalStorageDirectory() +
				"/Android/data/" +
				context.getPackageName() +
				dir);

			if (extMediaDir.exists()) {
				createNomediaDotFile(context, extMediaDir);
				return extMediaDir;
			}

			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				File sdcard = Environment.getExternalStorageDirectory();

				if (sdcard.canWrite()) {
					extMediaDir.mkdirs();
					createNomediaDotFile(context, extMediaDir);
					return extMediaDir;
				} else {
					Log.error(LOGCAT_NAME, "SD card not writeable, unable to create directory: " + extMediaDir.getPath());
				}
			} else {
				return extMediaDir;
			}
		}
		return null;
	}
	
	private static void createNomediaDotFile(Context context, File directory) {
		if (context != null && directory != null) {	
			File nomedia = new File(directory, NOMEDIA_FILENAME);
			
			if (!nomedia.exists()) {
				try {
					nomedia.createNewFile();
				} catch (IOException e) {
					Log.exception(LOGCAT_NAME, "unable to create .nomedia file in " + directory.getPath(), e);
				}
			}
		}
	}
}
