package blackbox.external.logger;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hugonicolau on 13/11/2014.
 * 
 * Writes data to log file
 */

public class DataWriter extends AsyncTask<Void, Void, String> {

	private final static String SUBTAG = "DataWriter: ";

	private ArrayList<String> mData;
	private String mFilePath;
	private String mFolderPath;
	private boolean mIsSyncWriting;
	private File rename;
	private boolean toAppend;

	public DataWriter(ArrayList<String> data, String folderPath,
			String filePath, boolean sync, boolean toAppend) {
		mData = data;
		mFilePath = filePath;
		mFolderPath = folderPath;
		mIsSyncWriting = sync;
		rename = null;
		this.toAppend = toAppend;

		if (mIsSyncWriting) {

			if (mData != null && !mData.isEmpty()) {

				// creates file
				writeFile(toAppend);
			}
		}
	}

	public DataWriter(ArrayList<String> data, String folderPath,
			String filePath, boolean sync, boolean toAppend,
			File renamePostExecute) {
		mData = data;
		mFilePath = filePath;
		mFolderPath = folderPath;
		mIsSyncWriting = sync;
		rename = renamePostExecute;
		this.toAppend = toAppend;

		if (mIsSyncWriting) {

			if (mData != null && !mData.isEmpty()) {

				// creates file
				writeFile(toAppend);
			}
		}
	}

	private void writeFile(boolean toAppend) {
		if (mFolderPath != null && !mFolderPath.isEmpty() && mFilePath!=null) {
			// creates folder
			File folder = new File(mFolderPath);
			if (!folder.exists())
				folder.mkdirs();

			File file = new File(mFilePath);
			FileWriter fw;

			try {
				fw = new FileWriter(file, toAppend);
				for (String line : mData) {
					fw.write(line + "\n");
				}
				fw.flush();
				fw.close();
				Log.v(BaseLogger.TAG, SUBTAG + mData.size()
						+ " Data write ok: " + file.getAbsolutePath()
						+ " toAppend: " + toAppend);
				mData = new ArrayList<String>();

			} catch (IOException e) {
				Log.v(BaseLogger.TAG,
						SUBTAG + "Data write BROKEN: " + file.getAbsolutePath()
								+ " " + e.getMessage());
			}
		}
	}

	@Override
	protected String doInBackground(Void... params) {
		if (mIsSyncWriting)
			return null;

		if (mData != null && !mData.isEmpty()) {

			// creates file
			writeFile(toAppend);
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
}