package tbb.core.logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import blackbox.external.logger.BaseLogger;
import tbb.core.CoreController;
import tbb.core.service.TBBService;

/**
 * Created by kyle montague on 10/11/2014.
 */
public class StorageCoordinator extends BroadcastReceiver {
	private static String SUBTAG = "StorageCoordinator: ";

	static String PREF_SEQUENCE_NUMBER = "BB.STORAGECOORDINATOR.PREFERENCE.SEQUENCE_NUMBER";

	private SharedPreferences mPref;
	private static int mSequence;
	private static String mAdjust;
	// is charging
	private static boolean isCharging = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			
			
			if (intent.getAction().equals(TBBService.ACTION_SCREEN_ON)
					|| intent.getAction().equals(CoreController.ACTION_INIT)) {
				Log.v(TBBService.TAG, SUBTAG + "screen ON");

				CoreController.sharedInstance().startServiceNoBroadCast();

				mPref = PreferenceManager
						.getDefaultSharedPreferences(CoreController
								.sharedInstance().getTBBService());

				mSequence = mPref.getInt(PREF_SEQUENCE_NUMBER, 0);
				mAdjust = adjust(mSequence);

				// CREATE FOLDER
				String folderPath = TBBService.STORAGE_FOLDER + "/" + mAdjust;
				File folder = new File(folderPath);
				if (!folder.exists())
					folder.mkdirs();

				// ANNOUNCE THE FOLDER AND SEQUENCE
				Intent intentUpdate = new Intent();
				intentUpdate
						.putExtra(BaseLogger.EXTRAS_FOLDER_PATH, folderPath);
				intentUpdate.putExtra(BaseLogger.EXTRAS_SEQUENCE, ""
						+ mSequence);
				intentUpdate.setAction(BaseLogger.ACTION_UPDATE);
				intentUpdate.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
				context.sendBroadcast(intentUpdate);

				// UPDATE THE SEQUENCE NUMBER FOR NEXT TIME
				mPref.edit().putInt(PREF_SEQUENCE_NUMBER, mSequence + 1)
						.commit();

			} else if (intent.getAction().equals(TBBService.ACTION_SCREEN_OFF)
					|| intent.getAction().equals(CoreController.ACTION_STOP)) {
				
				Log.v(TBBService.TAG, SUBTAG + "screen OFF");

				// ANNOUNCE THE FOLDER AND SEQUENCE
				Intent intentFlush = new Intent();
				intentFlush.setAction(BaseLogger.ACTION_FLUSH);
				intentFlush.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
				context.sendBroadcast(intentFlush);
				// TODO TESTING THIS.
				CoreController.sharedInstance().stopServiceNoBroadCast();

				// encrypt all descriptions and text while charging and screen
				// off or if it has passed 3 days and battery at 90+%
				Encryption.sharedInstance().encryptFolders(isCharging,context, mSequence);

				// Tell the cloud storage to sync
				CloudStorage.sharedInstance().cloudSync(
						TBBService.STORAGE_FOLDER, mSequence, false);
				
				//Logs first screen off
				if(!TBBService.isRunning){					
					MessageLogger.sharedInstance().writeAsync("TBB Service init");
					MessageLogger.sharedInstance().onFlush();
					TBBService.isRunning=true;
				}

			} else if (intent.getAction()
					.equals(BaseLogger.ACTION_SEND_REQUEST)) {
				// An external logger has been started manually and is
				// requesting the current sequence and location info.
				Log.v(TBBService.TAG, SUBTAG + "received location request");

				mAdjust = adjust(mSequence);
				String folderPath = TBBService.STORAGE_FOLDER + "/" + mAdjust;

				// ANNOUNCE THE FOLDER AND SEQUENCE
				Intent intentLocation = new Intent();
				intentLocation.putExtra(BaseLogger.EXTRAS_FOLDER_PATH,
						folderPath);
				intentLocation.putExtra(BaseLogger.EXTRAS_SEQUENCE, ""
						+ mSequence);
				intentLocation.setAction(BaseLogger.ACTION_LOCATION);
				intentLocation.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
				context.sendBroadcast(intentLocation);

			} else if (intent.getAction().equals(
					TBBService.ACTION_POWER_CONNECTED)) {
				isCharging = true;
			} else if (intent.getAction().equals(
					TBBService.ACTION_POWER_DISCONNECTED)) {
				isCharging = false;

			}
		} catch (Exception e) {
			Toast.makeText(CoreController.sharedInstance().getTBBService(),
					"TBB Exception", Toast.LENGTH_LONG).show();
			TBBService.writeToErrorLog(e);
		}
		
		
	}

	// TODO use timestamps
	private String adjust(int sequence) {

		if (sequence < 10) {
			return "0000" + sequence;
		} else if (sequence < 100) {
			return "000" + sequence;
		} else if (sequence < 1000)
			return "00" + sequence;
		else if (sequence < 10000)
			return "0" + sequence;

		return "" + sequence;

	}

}
