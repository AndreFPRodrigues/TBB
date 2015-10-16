package tbb.core.service;

import blackbox.tinyblackbox.R;
import tbb.core.CoreController;
import tbb.core.logger.CloudStorage;
import tbb.core.logger.Encryption;
import tbb.core.logger.MessageLogger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.util.Log;


import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class TBBPreferencesActivity extends PreferenceActivity {

    Preference.OnPreferenceClickListener preferenceClickListener;

	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
        MessageLogger.sharedInstance().requestStorageInfo(getApplicationContext());

        preferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences mPref = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                int mSequence = mPref.getInt(getResources().getString(R.string.PREF_SEQUENCE_NUMBER), 0);

                if(preference.getKey().equals(getResources().getString(R.string.BB_PREFERENCE_FORCE_ENCRYPT))){

                    Encryption.sharedInstance().encryptFolders(true,getApplicationContext(),mSequence);
                    Toast.makeText(getApplicationContext(),"Started encryption from log sequence "+mSequence,Toast.LENGTH_SHORT).show();
                    return true;
                }else if(preference.getKey().equals(getResources().getString(R.string.BB_PREFERENCE_FORCE_SYNC))){
                    CloudStorage.sharedInstance().cloudSync(TBBService.STORAGE_FOLDER,mSequence, true);
                    Toast.makeText(getApplicationContext(), "Started synchronisation from log sequence " + mSequence, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };

        Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                if(preference.getKey().equals(getResources().getString(R.string.BB_PREFERENCE_FLAG_RESEARCHER_SESSION))) {
                    boolean checked = Boolean.valueOf(o.toString());
                    String state = (checked)?"started":"ended";
                    String data = "Message:{type:study,timestamp:" + System.currentTimeMillis() + ",data:{text:researcher session, state:"+state+"}}";
                    MessageLogger.sharedInstance().writeAsync(data);
                    MessageLogger.sharedInstance().onFlush();
                    return true;
                }else{
                    //adding to the shared preferences if the user wants or not to record IO
                    //TODO stop and start io logging in real time when we change the preference
                    if(preference.getKey().equals(getResources().getString(R.string.BB_PREFERENCE_LOGIO))) {
                        boolean checked = Boolean.valueOf(o.toString());
                        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean(
                                getString(R.string.BB_PREFERENCE_LOGIO), checked);
                        editor.commit();
                        return true;
                    }

                    }
                return false;
            }
        };

        CheckBoxPreference researcher_pref = (CheckBoxPreference)findPreference(getResources().getString(R.string.BB_PREFERENCE_FLAG_RESEARCHER_SESSION));
        CheckBoxPreference log_io_pref = (CheckBoxPreference)findPreference(getResources().getString(R.string.BB_PREFERENCE_LOGIO));

        Preference sync_pref = findPreference(getResources().getString(R.string.BB_PREFERENCE_FORCE_SYNC));
        Preference encrypt_pref = findPreference(getResources().getString(R.string.BB_PREFERENCE_FORCE_ENCRYPT));


        researcher_pref.setOnPreferenceChangeListener(preferenceChangeListener);
        log_io_pref.setOnPreferenceChangeListener(preferenceChangeListener);

        sync_pref.setOnPreferenceClickListener(preferenceClickListener);
        encrypt_pref.setOnPreferenceClickListener(preferenceClickListener);

    }
}
