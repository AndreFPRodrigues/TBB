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
import blackbox.external.logger.DataWriter;

import tbb.core.service.TBBService;

public class Encryption {

	// shared instance for singleton design pattern
	private static Encryption mSharedInstance = null;
	private final static String SUBTAG = "Encrypt: ";

	private String key = "isTheStraightLin";
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
									File tree = new File(treeFolder
											+ curSequence + "_Tree.txt");
									File treeEnc = new File(treeFolder
											+ curSequence + "_Tree_E.txt");
									File keyStrokes = new File(keystrokesFolder
											+ curSequence + "_KeyStrokes.txt");
									File keyStrokesEnc = new File(
											keystrokesFolder + curSequence
													+ "_KeyStrokes_E.txt");
									File interaction = new File(treeFolder
											+ curSequence + "_Interaction.txt");
									File interactionEnc = new File(treeFolder
											+ curSequence
											+ "_Interaction_E.txt");

									// verify if there is a tree folder to
									// encrypt
									if (tree.exists()) {
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

	// encrypting interaction log
	// rewrites the interaction file
	private void encryptInteractionFile(File interaction, String path,
			File interactionEnc) {
		try {
			String toEncrypt;
			String[] toEncryptArray;
			ArrayList<String> newInteraction = new ArrayList<String>();

			InputStream inputStream = new FileInputStream(interaction);

			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(
						inputStream);
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				while ((toEncrypt = bufferedReader.readLine()) != null) {
					// delimeter only encrypt text
					// delimeter only encrypt text
					toEncryptArray = toEncrypt.split("!_!");
					if (toEncryptArray.length > 1) {
						String[] toEnc;
						if ((toEnc = toEncryptArray[0].split("!*!")).length > 1) {
							newInteraction
									.add("* "
											+ Base64.encodeBase64String(encrypt(toEncryptArray[0]))
											+ "," + toEncryptArray[1]);
						} else {
							newInteraction
									.add(Base64
											.encodeBase64String(encrypt(toEncryptArray[0]))
											+ "," + toEncryptArray[1]);
						}
					}
				}
				DataWriter w = new DataWriter(newInteraction, path,
						interaction.getAbsolutePath(), false, false,
						interactionEnc);
				w.execute();
			}
			inputStream.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// encrypting keystrokes
	// rewrites the keystroke file
	private void encryptKeyStrokesFile(File keyStrokes, String path,
			File keyStrokesEnc) {
		try {
			String toEncrypt;
			String[] toEncryptArray;
			ArrayList<String> newKeys = new ArrayList<String>();

			InputStream inputStream = new FileInputStream(keyStrokes);

			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(
						inputStream);
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				while ((toEncrypt = bufferedReader.readLine()) != null) {
					// delimeter only encrypt text
					toEncryptArray = toEncrypt.split(",");
					if (toEncryptArray.length > 1) {
						newKeys.add(Base64
								.encodeBase64String(encrypt(toEncryptArray[0]))
								+ "," + toEncryptArray[1]);
					}
				}
				DataWriter w = new DataWriter(newKeys, path,
						keyStrokes.getAbsolutePath(), false, false,
						keyStrokesEnc);
				w.execute();
				inputStream.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Encrypt the tree file, encrypts all the text and descriptions of all
	// nodes in all trees
	// Rewrites the tree file
	private void encryptTreeFile(File tree, String path, File treeEnc) {
		Log.v(TBBService.TAG, SUBTAG + "To encrypt" + path);
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
			DataWriter w = new DataWriter(newTree, path,
					tree.getAbsolutePath(), false, false, treeEnc);
			w.execute();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
				+ "_Tree.txt");
		File keyStrokes = new File(sequenceFolder.getAbsolutePath()
				+ "/KeyStrokes/" + seq + "_KeyStrokes.txt");

		if (tree.exists() || keyStrokes.exists()) {
			return false;
		} else {
			return true;
		}
	}

}