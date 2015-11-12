package tbb.core.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Scanner;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import blackbox.external.logger.DataWriter;

import tbb.core.service.TBBService;

/**
 * TODO THIS IS NOT BEING USED RIGHT NOW!
 */

public class Encryption {

	// shared instance for singleton design pattern
	private static Encryption mSharedInstance = null;
	private final static String SUBTAG = "Encrypt: ";

	private String key = "keytobegenerated";
	private long mLastEnc = 0;
	private final static long ENCRYPT_THRESHOLD = 1000 * 60 * 60 * 24 * 3; // every
																			// 3days
	private final static float BATTERY_THRESHOLD = 80;

	public static Encryption sharedInstance() {
		if (mSharedInstance == null)
			mSharedInstance = new Encryption();
		return mSharedInstance;
	}

	protected Encryption() {
		mLastEnc = 0;
	}

	//todo proper encryption
	public byte[] encrypt(String value) throws GeneralSecurityException {

		byte[] raw = key.getBytes(Charset.forName("US-ASCII"));
		if (raw.length != 16) {
			throw new IllegalArgumentException("Invalid key size.");
		}

		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(
				new byte[16]));
		return cipher.doFinal(value.getBytes(Charset.forName("US-ASCII")));
	}

	public String decrypt(byte[] encrypted) throws GeneralSecurityException {

		byte[] raw = key.getBytes(Charset.forName("US-ASCII"));
		if (raw.length != 16) {
			throw new IllegalArgumentException("Invalid key size.");
		}
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(
				new byte[16]));
		byte[] original = cipher.doFinal(encrypted);

		return new String(original, Charset.forName("US-ASCII"));
	}

	public void encryptFolders(boolean isCharging, Context context,
			final int sequence) {
		Time now = new Time();
		now.setToNow();

		// if is charging or we havent encrypted in 3 days and the battery is
		// above the threshold
		if (isCharging
				|| (Math.abs(mLastEnc - now.toMillis(false)) > ENCRYPT_THRESHOLD && getBatteryLevel(context
						.getApplicationContext()) > BATTERY_THRESHOLD)) {
			Log.v(TBBService.TAG, SUBTAG + "Start with folders less than: "
					+ sequence);
			// launch new thread
			Thread b = new Thread(new Runnable() {
				public void run() {

					File TBBFolder = new File(TBBService.STORAGE_FOLDER);
					if (TBBFolder.exists()) {
						// for each folder
						for (File sequenceFolder : TBBFolder.listFiles()) {

							// make sure it is a sequence (interaction session)
							// folder
							if (sequenceFolder.isDirectory()
									&& isInteger(sequenceFolder.getName())) {
								String currentFolder = sequenceFolder
										.getAbsolutePath();
								String treeFolder = currentFolder + "/Tree/";
								String keystrokesFolder = currentFolder
										+ "/Keystrokes/";

								// get sequence number
								int curSequence = Integer
										.parseInt(sequenceFolder.getName());
								if (curSequence < (sequence - 1)) {
									// TODO sequence - 2 folder can still be being written?
									File tree = new File(treeFolder
											+ curSequence + "_Tree.json");
									File treeEnc = new File(treeFolder
											+ curSequence + "_Tree_E.json");
									File keyStrokes = new File(keystrokesFolder
											+ curSequence + "_KeyStrokes.json");
									File keyStrokesEnc = new File(
											keystrokesFolder + curSequence
													+ "_KeyStrokes_E.json");
									File interaction = new File(treeFolder
											+ curSequence + "_Interaction.json");
									File interactionEnc = new File(treeFolder
											+ curSequence
											+ "_Interaction_E.json");
									/*Log.d(TBBService.TAG, "encrypting:" + keystrokesFolder
											+ curSequence + "_KeyStrokes.json");*/

									// verify if there is a tree folder to
									// encrypt
									if (tree.exists()) {
										Log.d(TBBService.TAG, "Encrypting tree");

										encryptTreeFile(tree, currentFolder,
												treeEnc);
									}

									// verify if there is a keystrokes folder to
									// encrypt
									if (keyStrokes.exists()) {
										encryptKeyStrokesFile(keyStrokes,
												currentFolder, keyStrokesEnc);
									}

									// verify if there is a interaction folder
									// to
									// encrypt
									if (interaction.exists()) {
										encryptInteractionFile(interaction,
												currentFolder, interactionEnc);
									}
								}
							}
						}
					}
					Time nowE = new Time();
					nowE.setToNow();
					mLastEnc = nowE.toMillis(false);
					Log.v(TBBService.TAG, SUBTAG + "Successfull");
				}

			});
			b.start();
		}
	}

	// Retrive battery level
	private float getBatteryLevel(Context c) {
		Intent batteryIntent = c.registerReceiver(null, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		// Error checking that probably isn't needed but I added just in case.
		if (level == -1 || scale == -1) {
			return 50.0f;
		}
		return ((float) level / (float) scale) * 100.0f;
	}

	//Loads json object from file in path
	public JSONObject loadJSON(String path) {
		String json = null;
		try {

			InputStream is = new FileInputStream(path);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			json = new String(buffer, "UTF-8");
			return  new JSONObject(json);
		} catch (IOException ex) {
			ex.printStackTrace();
		}catch (JSONException e) {
			e.printStackTrace();
		}
		return null;

	}
	// encrypting interaction log
	// rewrites the interaction file
	private void encryptInteractionFile(File interaction, String path,
			File interactionEnc) {
		try {
				JSONObject intJson = loadJSON(interaction.getPath());
				//Log.d(TBBService.TAG, "Encrypt 1:" + keystrokesJson.getString("text"));
				JSONArray intArray = intJson.getJSONArray("records");
				for (int i = 0; i < intArray.length(); i++) {
					if (intArray.getJSONObject(i).has("desc")) {
						String desc = intArray.getJSONObject(i).getString("desc");
						if (desc.length() > 0) {
							desc = Base64.encodeBase64String(encrypt(desc));
							intArray.getJSONObject(i).put("desc", desc);
						}
					}
				}
				String jsonString = intJson.toString();
				String[] arrayString = new String[1];
				arrayString[0] = jsonString;
				DataWriter w = new DataWriter(path, interaction.getAbsolutePath(), false,
						interactionEnc, false);
				w.execute(jsonString);


			}catch (GeneralSecurityException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

	}

	// encrypting keystrokes
	// rewrites the keystroke file
	private void encryptKeyStrokesFile(File keyStrokes, String path,
			File keyStrokesEnc) {

		try {
			JSONObject keystrokesJson = loadJSON(keyStrokes.getPath());
			//Log.d(TBBService.TAG, "Encrypt 1:" + keystrokesJson.getString("text"));
			JSONArray keyArray = keystrokesJson.getJSONArray("records");
			for (int i = 0; i < keyArray.length(); i++) {
				if (keyArray.getJSONObject(i).has("text")) {
					String text = keyArray.getJSONObject(i).getString("text");
					String keystroke = keyArray.getJSONObject(i).getString("keystroke");
					if (text.length() > 0) {
						text = Base64.encodeBase64String(encrypt(text));
						keyArray.getJSONObject(i).put("text", text);
					}
					if (keystroke.length() > 0) {
						keystroke = Base64.encodeBase64String(encrypt(keystroke));
						keyArray.getJSONObject(i).put("keystroke", keystroke);
					}
				}
			}
			String jsonString = keystrokesJson.toString();
			String[] arrayString = new String[1];
			arrayString[0] = jsonString;
			DataWriter w = new DataWriter(path, keyStrokes.getAbsolutePath(), false,
					keyStrokesEnc, false);
			w.execute(jsonString);


		}catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	// Encrypt the tree file, encrypts all the text and descriptions of all
	// nodes in all trees
	// Rewrites the tree file
	private void encryptTreeFile(File tree, String path, File treeEnc) {
		try {
			JSONObject treeJson = loadJSON(tree.getPath());
			Log.d(TBBService.TAG, "Encrypt tree:" + tree.getPath());
			JSONArray treeArray = treeJson.getJSONArray("records");
			for (int i = 0; i < treeArray.length(); i++) {
				if(treeArray.getJSONObject(i).has("tree")) {
					JSONObject treeObject = treeArray.getJSONObject(i).getJSONObject("tree");
					treeArray.getJSONObject(i).put("encripted", true);
					encriptNode(treeObject);
					encriptChildren(treeObject);
				}
			}

			String jsonString = treeJson.toString();
			String[] arrayString = new String[1];
			arrayString[0] = jsonString;
			DataWriter w = new DataWriter(path, tree.getAbsolutePath(), false,
					treeEnc, false);
			w.execute(jsonString);


		} catch (JSONException e) {
			e.printStackTrace();
		}


		/*Log.v(TBBService.TAG, SUBTAG + "To encrypt" + path);
		try {
			String[] toEncryptArray;
			ArrayList<String> newTree = new ArrayList<String>();
			String toEncrypt = "";

			InputStream inputStream = new FileInputStream(tree);

			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(
						inputStream);
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				while ((toEncrypt = bufferedReader.readLine()) != null) {
					// delimeter added to the text and description of every node
					// that is not null
					toEncryptArray = toEncrypt.split("!_te_!");
					if (toEncryptArray.length > 1) {
						newTree.add(encryptTree(toEncryptArray));
					} else {
						newTree.add(toEncrypt);
					}
					toEncrypt = null;
					toEncryptArray = null;
				}

				inputStream.close();
			}
			DataWriter w = new DataWriter(path, tree.getAbsolutePath(), false, treeEnc);
			w.execute(newTree.toArray(new String[newTree.size()]));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/

	}

	private void encriptChildren(JSONObject parent){
		try {
			JSONArray children=parent.getJSONArray("children");
			for (int i = 0; i < children.length(); i++) {
				JSONObject child = children.getJSONObject(i);
				encriptNode(child);
				encriptChildren(child);
			}
			} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void encriptNode(JSONObject node) {
		try {
			String text = node.getString("text");
			String content = node.getString("content");

			if (!text.equals("null")) {
				text = Base64.encodeBase64String(encrypt(text));
				node.put("text", text);
			}
			if (!content.equals("null")) {
				content = Base64.encodeBase64String(encrypt(content));
				node.put("content", content);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		} catch (GeneralSecurityException e1) {
			e1.printStackTrace();
		}


	}

	// Encrypt the text:"" and description:"" of every node in the given tree
	private String encryptTree(String[] toEncryptArray) {
		String result = "";

		for (int i = 0; i < toEncryptArray.length; i++) {
			// the texts to encrypt are surround by our delimiter
			if ((i % 2) == 0) {

				result += toEncryptArray[i];
			} else {
				try {
					String text = Base64
							.encodeBase64String(encrypt(toEncryptArray[i]));
					result += text;
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
				}
			}
		}

		return result;
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

	public boolean isEncrypted(File sequenceFolder) {
		int seq = Integer.parseInt(sequenceFolder.getName());
		File tree = new File(sequenceFolder.getAbsolutePath() + "/Tree/" + seq
				+ "_Tree.json");
		File keyStrokes = new File(sequenceFolder.getAbsolutePath()
				+ "/KeyStrokes/" + seq + "_KeyStrokes.json");

		if (tree.exists() || keyStrokes.exists()) {
			return false;
		} else {
			return true;
		}
	}

}