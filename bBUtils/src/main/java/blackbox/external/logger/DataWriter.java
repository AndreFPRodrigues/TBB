package blackbox.external.logger;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by hugonicolau on 13/11/2014.
 * 
 * Writes data to log file
 */

public class DataWriter extends AsyncTask<String, Void, String> {

	private final static String SUBTAG = "DataWriter: ";

	private String mFilePath;
	private String mFolderPath;
	private File rename;
	private boolean toAppend;

	public DataWriter(String folderPath, String filePath, boolean toAppend) {
		mFilePath = filePath;
		mFolderPath = folderPath;
		rename = null;
		this.toAppend = toAppend;
	}

	public DataWriter(String folderPath, String filePath, boolean toAppend, File renamePostExecute) {
		mFilePath = filePath;
		mFolderPath = folderPath;
		rename = renamePostExecute;
		this.toAppend = toAppend;
	}

	@Override
	protected String doInBackground(String... data) {

		if (data != null && data.length > 0) {

			// creates file
			writeFile(data, toAppend);
		}

		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		if (rename != null) {
			File file = new File(mFilePath);
			file.renameTo(rename);
			Log.v(BaseLogger.TAG, SUBTAG + "Successfully renamed");
		}
	}

	private void writeFile(String[] data, boolean toAppend) {
		if (mFolderPath != null && !mFolderPath.isEmpty() && mFilePath!=null) {
			// creates folder
			File folder = new File(mFolderPath);
			if (!folder.exists())
				folder.mkdirs();

			File file = new File(mFilePath);
			FileWriter fw;

			try {
				fw = new FileWriter(file, toAppend);
				for (String line : data) {
					fw.write(line + "\n");
				}
				fw.flush();
				fw.close();
				Log.v(BaseLogger.TAG, SUBTAG + data.length
						+ " Data write ok: " + file.getAbsolutePath()
						+ " toAppend: " + toAppend);

			} catch (IOException e) {
				Log.v(BaseLogger.TAG,
						SUBTAG + "Data write BROKEN: " + file.getAbsolutePath()
								+ " " + e.getMessage());
			}
		}
	}

}