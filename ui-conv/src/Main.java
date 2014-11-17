import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Main {
	private static final String DRAWABLE_DIRECTORY_FILENAME = "drawable";
	private static final String DRAWABLE_MDPI_DIRECTORY_FILENAME = "drawable-mdpi";
	private static final String DRAWABLE_HDPI_DIRECTORY_FILENAME = "drawable-hdpi";
	private static final String DRAWABLE_XHDPI_DIRECTORY_FILENAME = "drawable-xhdpi";

	private static final String MDPI_FILE_MARK = "_1";
	private static final String HDPI_FILE_MARK = "_1.5";
	private static final String XHDPI_FILE_MARK = "_2";

	private File mSrcDir;
	private File mOutDir;

	public static void main(String[] args) throws Exception {
		new Main().run();
	}

	public void run() throws Exception {
		mSrcDir = new File(".");
		mOutDir = new File(mSrcDir, "res");
		mOutDir.mkdir();

		new File(mOutDir, DRAWABLE_DIRECTORY_FILENAME).mkdir();
		new File(mOutDir, DRAWABLE_MDPI_DIRECTORY_FILENAME).mkdir();
		new File(mOutDir, DRAWABLE_HDPI_DIRECTORY_FILENAME).mkdir();
		new File(mOutDir, DRAWABLE_XHDPI_DIRECTORY_FILENAME).mkdir();

		convertDirectory(mSrcDir);
		convertDirectory(new File(mSrcDir, "Attach"));
		convertDirectory(new File(mSrcDir, "Audio"));
		convertDirectory(new File(mSrcDir, "Profile"));
		convertDirectory(new File(mSrcDir, "Tabbar"));
		convertDirectory(new File(mSrcDir, "Login"));
		convertDirectory(new File(mSrcDir, "Mass_actions"));
		convertDirectory(new File(mSrcDir, "Loaders"), "load_");
	}

	private void convertDirectory(File dir) throws Exception{
		convertDirectory(dir, null);
	}

	private void convertDirectory(File dir, String dstFilePrefix) throws Exception {
		for (File source : dir.listFiles()) {
			if (source.isDirectory()) continue;
			if (!source.getName().endsWith("png")) continue;

			File destination = getDestinationFile(source, dstFilePrefix);

			System.out.println(source.getAbsolutePath() + "->" + destination.getAbsolutePath());

			copyFile(source, destination);
		}		
	}

	private File getDestinationFile(File source, String dstFilePrefix) {
		String srcFn = source.getName();
		String dstFn = null;
		String dstFolder = DRAWABLE_DIRECTORY_FILENAME;

		if (isForLdpi(srcFn)) {
			dstFolder = DRAWABLE_MDPI_DIRECTORY_FILENAME;
		} else if (isForMdpi(srcFn)) {
			dstFolder = DRAWABLE_HDPI_DIRECTORY_FILENAME;
		} else if (isForHdpi(srcFn)) {
			dstFolder = DRAWABLE_XHDPI_DIRECTORY_FILENAME;
		}

		if (srcFn.startsWith("Con_")) {
			srcFn = srcFn.replace("Con_", "conference_");
		}

		dstFn = srcFn.toLowerCase().replace(XHDPI_FILE_MARK, "").replace(HDPI_FILE_MARK, "").replace(MDPI_FILE_MARK, "").replace("__", "_");

		if (dstFilePrefix != null) {
			dstFn = dstFilePrefix + dstFn;
		}

		return new File(new File(mOutDir, dstFolder), dstFn);
	}

	private static void copyFile(File f1, File f2) throws IOException {
		InputStream in = new FileInputStream(f1);
		OutputStream out = new FileOutputStream(f2);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0){
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private static String removeExt(String filename) {
		return filename.substring(0, filename.length() - 4);
	}

	private static boolean isForLdpi(String filename) {
		return removeExt(filename).endsWith(MDPI_FILE_MARK);
	}

	private static boolean isForMdpi(String filename) {
		return removeExt(filename).endsWith(HDPI_FILE_MARK);
	}

	private static boolean isForHdpi(String filename) {
		return removeExt(filename).endsWith(XHDPI_FILE_MARK);
	}
}
