package tbb.core.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;

import tbb.core.CoreController;
import tbb.core.service.TBBService;

/**
 * Simple wrapper around the Google Cloud Storage API
 */
public class CloudStorage {

	private static final String SUBTAG = "CloudStorage: ";

	// shared instance for singleton design pattern
	private static CloudStorage mSharedInstance = null;

	// cloud storage keys
	private static final String PROJECT_ID_PROPERTY = "project.id";
	private static final String APPLICATION_NAME_PROPERTY = "application.name";
	private static final String ACCOUNT_ID_PROPERTY = "account.id";
	private static final String PRIVATE_KEY_PATH_PROPERTY = "private.key.path";

	// variables used to upload files to the cloud
	private Properties mProperties;
	private Storage mStorage;

	// timestamp for last successful upload
	private Long mLastSync = (long) 0;
	private long mLastZip = 0;

	// threshold to try to upload
	private final static long SYNC_THRESHOLD = 1000 * 60 * 60 * 24; // every 24h
	// used to guarantee that all loggers flush their data
	private final static long ZIPPING_THRESHOLD = 10 * 60 * 1000; // 10m

	protected CloudStorage() {
		mLastSync = (long) 0;
		mLastZip = 0;
	}

	public static CloudStorage sharedInstance() {
		if (mSharedInstance == null)
			mSharedInstance = new CloudStorage();
		return mSharedInstance;
	}

	public void cloudSync(final String path, final int sequence, boolean force) {
		Log.v(TBBService.TAG, SUBTAG + " cloudSync()");
		pingServer(CoreController.sharedInstance().getTBBService(), sequence, CoreController.sharedInstance().permission);
		Time now = new Time();
		now.setToNow();

		// if we already tried to sync and zip files "today", then ignore
		if (!force
				&& Math.abs(mLastSync - now.toMillis(false)) < SYNC_THRESHOLD
				&& Math.abs(mLastZip - now.toMillis(false)) < SYNC_THRESHOLD)
			return;

		// launch new thread
		Thread b = new Thread(new Runnable() {
			public void run() {
				try {

					// synchronized access to guarantee that we don't run the
					// code twice
					// while zipping or uploading
					synchronized (mLastSync) {
						Time now = new Time();
						now.setToNow();

						// do all the zipping
						// zip files even when there isn't internet connection
						if (Math.abs(mLastZip - now.toMillis(false)) >= SYNC_THRESHOLD) {
							if (ZipPreviousSequences(path, sequence)) {
								now.setToNow();
								mLastZip = now.toMillis(false);
								// TODO sanity check, save last successful zip
								// sequence and use it on next
								// ZipPreviousSequences
							}
						}

						// try to upload
						// TODO check battery
						if (isNetworkConnected(CoreController.sharedInstance()
								.getTBBService())) {
							Log.v(TBBService.TAG, SUBTAG
									+ "verify files to upload");
							String bucket = checkBucket(CoreController
									.sharedInstance().getTBBService());
							Log.v(TBBService.TAG, SUBTAG
									+ " trying to delete...");
							syncFiles(bucket);
							Log.v(TBBService.TAG, SUBTAG
									+ " trying to upload...");

							uploadFiles(bucket);
							now.setToNow();
							mLastSync = now.toMillis(false);
						}
					}
				} catch (Exception e) {
					Log.v(TBBService.TAG,
							SUBTAG + "Exception while trying to upload files "
									+ e.getMessage());
					TBBService.writeToErrorLog(e);

				}
			}
		});
		b.start();
	}

	private void pingServer(final Context context, final int sequence, final boolean permission) {
		Thread b = new Thread(new Runnable() {
			public void run() {
				try {
					Log.v(TBBService.TAG, SUBTAG + " Pinging");
					String permissions = "GRANTED";
					if(!permission)
						permissions="DENIED";
					
					String account = getAccount(context);
					if (isNetworkConnected(CoreController.sharedInstance()
							.getTBBService())) {
						Log.v(TBBService.TAG, SUBTAG + "connected Pinging");
						
						HttpClient httpclient = new DefaultHttpClient();
						HttpPost httppost = new HttpPost(
								"http://accessible-serv.lasige.di.fc.ul.pt/~lost/log/uploadTBB.php");
						SimpleDateFormat s = new SimpleDateFormat(
								"dd-MM-yyyy hh:mm:ss");
						String format = s.format(new Date());

						List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
								2);
						nameValuePairs.add(new BasicNameValuePair("account",
								account));
						nameValuePairs.add(new BasicNameValuePair("date",
								format));
						nameValuePairs.add(new BasicNameValuePair("sequence",
								sequence + " " + permissions));
						httppost.setEntity(new UrlEncodedFormEntity(
								nameValuePairs));

						httpclient.execute(httppost);

					}
				} catch (Exception e) {
					
					TBBService.writeToErrorLog(e);
					//e.printStackTrace();
				}
			}
		});
		b.start();

	}

	private boolean ZipPreviousSequences(String folderPath, int sequence) {
		Log.v(TBBService.TAG, SUBTAG + " Zipping folders less than: "
				+ folderPath + " sequence: " + sequence);

		ArrayList<String> toZipList = new ArrayList<String>();

		// TODO define global variable for threshold
		Time now = new Time();
		now.setToNow();
		long Threshold = now.toMillis(false) - ZIPPING_THRESHOLD; // zip from 10
																	// minutes
																	// ago

		// go through all sequence folders in TBB parent folder and add them to
		// the list to zip
		File TBBFolder = new File(TBBService.STORAGE_FOLDER);
		if (TBBFolder.exists()) {
			// these variables will be used to create the .zip name
			int lowestSequence = Integer.MAX_VALUE;
			int highestSequence = Integer.MIN_VALUE;

			// for each folder
			for (File sequenceFolder : TBBFolder.listFiles()) {
				// seperate the encrypted tag

				// make sure it is a sequence (interaction session) folder
				if (sequenceFolder.isDirectory()
						&& isInteger(sequenceFolder.getName())) {
					// get sequence number
					int curSequence = Integer
							.parseInt(sequenceFolder.getName());
					// make sure it is not the current or last session, and
					// it was last changed more than X minutes ago (loggers may
					// still be writing)

					if (sequenceFolder.lastModified() <= Threshold
							&& curSequence < (sequence - 1)
							&& Encryption.sharedInstance().isEncrypted(
									sequenceFolder)) {
						Log.v(TBBService.TAG, SUBTAG + "zipping:" + curSequence
								+ " < " + sequence + " " + sequenceFolder);
						toZipList.add(sequenceFolder.getAbsolutePath());
						if (curSequence < lowestSequence)
							lowestSequence = curSequence;
						if (curSequence > highestSequence)
							highestSequence = curSequence;
					}
				}
			}

			if (toZipList.size() == 0) {
				Log.v(TBBService.TAG, SUBTAG + "no folders to zip");
				return false;
			}

			// zip sequence folders
			if (Zipping.Zip(toZipList, TBBService.STORAGE_FOLDER + "/"
					+ lowestSequence + "_" + highestSequence + "_Logs.zip")) {
				Log.v(TBBService.TAG, SUBTAG
						+ "zipping successful! going to delete folders");
				for (String sequenceFolder : toZipList) {
					DeleteRecursive(new File(sequenceFolder));
				}
			} else {
				Log.v(TBBService.TAG, SUBTAG + "Error, couldn't zip folders");
				return false;
			}
		}
		return true;
	}

	/**
	 * Uploads a file to a bucket. Filename and content type will be based on
	 * the original file.
	 * 
	 * @param bucketName
	 *            Bucket where file will be uploaded
	 * @param filePath
	 *            Absolute path of the file to upload
	 * @throws Exception
	 */
	public void uploadFile(String bucketName, String filePath) throws Exception {

		Storage storage = getStorage();

		StorageObject object = new StorageObject();
		object.setBucket(bucketName);

		File file = new File(filePath);

		InputStream stream = new FileInputStream(file);
		try {
			String contentType = URLConnection
					.guessContentTypeFromStream(stream);
			InputStreamContent content = new InputStreamContent(contentType,
					stream);
			content.setLength(file.length());

			Storage.Objects.Insert insert = storage.objects().insert(
					bucketName, null, content);
			insert.setName(file.getName());

			insert.execute();
		} finally {
			Log.v(TBBService.TAG, SUBTAG + "success");
			stream.close();
		}
	}

	public void downloadFile(String bucketName, String fileName,
			String destinationDirectory) throws Exception {

		File directory = new File(destinationDirectory);
		if (!directory.isDirectory()) {
			throw new Exception(
					"Provided destinationDirectory path is not a directory");
		}
		File file = new File(directory.getAbsolutePath() + "/" + fileName);

		Storage storage = getStorage();

		Storage.Objects.Get get = storage.objects().get(bucketName, fileName);
		FileOutputStream stream = new FileOutputStream(file);
		try {
			get.executeAndDownloadTo(stream);
		} finally {
			stream.close();
		}
	}

	/**
	 * Deletes a file within a bucket
	 * 
	 * @param bucketName
	 *            Name of bucket that contains the file
	 * @param fileName
	 *            The file to delete
	 * @throws Exception
	 */
	public void deleteFile(String bucketName, String fileName) throws Exception {

		Storage storage = getStorage();

		storage.objects().delete(bucketName, fileName).execute();
	}

	/**
	 * Creates a bucket
	 * 
	 * @param bucketName
	 *            Name of bucket to create
	 * @throws Exception
	 */
	public void createBucket(String bucketName) throws Exception {

		Storage storage = getStorage();

		Bucket bucket = new Bucket();
		bucket.setName(bucketName);

		storage.buckets()
				.insert(getProperties().getProperty(PROJECT_ID_PROPERTY),
						bucket).execute();
	}

	/**
	 * Deletes a bucket
	 * 
	 * @param bucketName
	 *            Name of bucket to delete
	 * @throws Exception
	 */
	public void deleteBucket(String bucketName) throws Exception {

		Storage storage = getStorage();

		storage.buckets().delete(bucketName).execute();
	}

	/**
	 * Lists the objects in a bucket
	 * 
	 * @param bucketName
	 *            bucket name to list
	 * @return Array of object names
	 * @throws Exception
	 */
	public List<String> listBucket(String bucketName) throws Exception {

		Storage storage = getStorage();

		List<String> list = new ArrayList<String>();

		List<StorageObject> objects = storage.objects().list(bucketName)
				.execute().getItems();
		if (objects != null) {
			for (StorageObject o : objects) {
				list.add(o.getName());
			}
		}

		return list;
	}

	/**
	 * List the buckets with the project (Project is configured in mProperties)
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<String> listBuckets() throws Exception {

		Storage storage = getStorage();

		List<String> list = new ArrayList<String>();

		List<Bucket> buckets = storage.buckets()
				.list(getProperties().getProperty(PROJECT_ID_PROPERTY))
				.execute().getItems();
		if (buckets != null) {
			for (Bucket b : buckets) {
				list.add(b.getName());
			}
		}

		return list;
	}

	private Properties getProperties() throws Exception {

		if (mProperties == null) {
			mProperties = new Properties();
			InputStream stream = CloudStorage.class
					.getResourceAsStream("/cloudstorage.properties");
			try {
				mProperties.load(stream);
			} catch (IOException e) {
				throw new RuntimeException(
						"cloudstorage.mProperties must be present in classpath",
						e);
			} finally {
				stream.close();
			}
		}
		return mProperties;
	}

	private static final String KEY_TYPE = "PKCS12";
	private static final String KEY_ALIAS = "privatekey";
	private static final String KEY_PASSWORD = "notasecret";

	private Storage getStorage() throws Exception {

		if (mStorage == null) {

			HttpTransport httpTransport = new NetHttpTransport();
			JsonFactory jsonFactory = new JacksonFactory();

			List<String> scopes = new ArrayList<String>();
			scopes.add(StorageScopes.DEVSTORAGE_FULL_CONTROL);

			KeyStore keyStore = KeyStore.getInstance(KEY_TYPE);
			InputStream keyStream = CloudStorage.class
					.getResourceAsStream("/key.p12");
			PrivateKey privateKey = PrivateKeys.loadFromKeyStore(keyStore,
					keyStream, KEY_PASSWORD, KEY_ALIAS, KEY_PASSWORD);

			Credential credential = new GoogleCredential.Builder()
					.setTransport(httpTransport)
					.setJsonFactory(jsonFactory)
					.setServiceAccountId(
							getProperties().getProperty(ACCOUNT_ID_PROPERTY))
					.setServiceAccountPrivateKey(privateKey)
					.setServiceAccountScopes(scopes).build();

			mStorage = new Storage.Builder(httpTransport, jsonFactory,
					credential).setApplicationName(
					getProperties().getProperty(APPLICATION_NAME_PROPERTY))
					.build();
		}

		return mStorage;
	}

	private String getAccount(Context context) throws Exception {
		AccountManager manager = (AccountManager) context
				.getSystemService(Context.ACCOUNT_SERVICE);
		Account[] list = manager.getAccounts();
		if (list.length > 0) {
			return ((list[0].name.split("@"))[0]).toLowerCase().replace(".", "");
		} 
		return "";  
	}
	 

	protected String checkBucket(Context context) throws Exception {

		List<String> buckets = listBuckets();
		String account = getAccount(context);
		if (account == null) {
			return null;
		}
		String buck = "tbb_" + account;
		for (String b : buckets) {
			if (b.equals(buck)) {
				return buck;
			}
		}
		createBucket(buck);
		return buck;

	}

	protected boolean uploadFiles(String bucket) throws Exception {
		String[] filesAndFolders = (new File(TBBService.STORAGE_FOLDER)).list();
		if (filesAndFolders == null)
			return false;
		ArrayList<String> zipFiles = new ArrayList<String>();

		for (int i = 0; i < filesAndFolders.length; i++) {
			// check if zips[i] is a .zip
			if (filesAndFolders[i].endsWith(".zip")
					|| filesAndFolders[i].endsWith(".ZIP")) {
				zipFiles.add(TBBService.STORAGE_FOLDER + "/"
						+ filesAndFolders[i]);
			}
		}

		if (zipFiles.size() == 0)
			return true;

		Log.v(TBBService.TAG, SUBTAG + "uploading files");

		String uploadName = zipFiles.get(zipFiles.size() - 1);
		int size = zipFiles.size();
		if (size > 1) {
			String last = zipFiles.get(size - 1);
			String [] sequenceArray = (last.split("_"));
			String sequence=sequenceArray[0];
			if(sequenceArray.length>1){
				sequence=sequenceArray[1];
			}
			uploadName = TBBService.STORAGE_FOLDER + "/" + "_MultipleLogs_"
					+ sequence + ".zip";
			if (!Zipping.Zip(zipFiles, uploadName))
				return false;
		}

		uploadFile(bucket, uploadName);

		return true;
	}

	// TODO review this code, da fuck is this?
	// Deletes all zip files that are stored in the cloud
	// Ensure we only delete when we know they are sync
	protected void syncFiles(String bucket) throws Exception {

		// TODO what's in files?
		List<String> files = listBucket(bucket);
		if (files.size() == 0) {
			return;
		}

		// TODO zips contain zip files and folders
		String[] zips = (new File(TBBService.STORAGE_FOLDER)).list();
		Arrays.sort(zips);

		// TODO what are we doing here?
		for (int j = 0; j < zips.length; j++) {

			if (zips[j].equals(files.get(files.size() - 1))) {
				for (int k = 0; k < j + 1; k++) {
					File del = new File(TBBService.STORAGE_FOLDER + "/"
							+ zips[k]);
					del.delete();

				}
				return;
			}

		}

	}

	/*
	 * UTILS METHODS
	 */

	private boolean isNetworkConnected(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
	}

	private void DeleteRecursive(File fileOrDirectory) {
		try {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				DeleteRecursive(child);

		fileOrDirectory.delete();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	// check whether the string is a valid integer
	private boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
