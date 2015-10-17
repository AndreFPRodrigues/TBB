package tbb.core.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import blackbox.tinyblackbox.R;
import tbb.core.logger.CloudStorage;
import tbb.core.logger.Encryption;
import tbb.core.logger.MessageLogger;
import tbb.core.service.configuration.AppPermissionListActivity;
import tbb.core.service.configuration.DataPermissions;

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
                int mSequence = mPref.getInt(getString(R.string.PREF_SEQUENCE_NUMBER), 0);

                if(preference.getKey().equals(getString(R.string.BB_PREFERENCE_FORCE_ENCRYPT))){

                    Encryption.sharedInstance().encryptFolders(true,getApplicationContext(),mSequence);
                    Toast.makeText(getApplicationContext(),"Started encryption from log sequence "+mSequence,Toast.LENGTH_SHORT).show();
                    return true;
                }else if(preference.getKey().equals(getString(R.string.BB_PREFERENCE_FORCE_SYNC))){
                    CloudStorage.sharedInstance().cloudSync(TBBService.STORAGE_FOLDER,mSequence, true);
                    Toast.makeText(getApplicationContext(), "Started synchronisation from log sequence " + mSequence, Toast.LENGTH_SHORT).show();
                    return true;
                }else if(preference.getKey().equals(getString(R.string.BB_PREFERENCE_APP_PERMISSIONS))){
                    Intent i = new Intent(getApplicationContext(), AppPermissionListActivity.class);
                    startActivity(i);
                    return true;
                }
                return false;
            }
        };

        Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                if(preference.getKey().equals(getString(R.string.BB_PREFERENCE_FLAG_RESEARCHER_SESSION))) {
                    boolean checked = Boolean.valueOf(o.toString());
                    String state = (checked)?"started":"ended";
                    String data = "Message:{type:study,timestamp:" + System.currentTimeMillis() + ",data:{text:researcher session, state:"+state+"}}";
                    MessageLogger.sharedInstance().writeAsync(data);
                    MessageLogger.sharedInstance().onFlush();
                    return true;
                }
                    //adding to the shared preferences if the user wants or not to record IO
                    //TODO stop and start io logging in real time when we change the preference
                else if(preference.getKey().equals(getString(R.string.BB_PREFERENCE_LOGIO))) {
                        boolean checked = Boolean.valueOf(o.toString());
                        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean(
                                getString(R.string.BB_PREFERENCE_LOGIO), checked);
                        editor.commit();
                        return true;
                }else if(preference.getKey().equals(getString(R.string.BB_PREFERENCE_ENCRYPTION_LEVEL))) {
                    getPreferences(MODE_PRIVATE).edit().putString(getString(R.string.BB_PREFERENCE_ENCRYPTION_LEVEL), o.toString()).commit();
                    DataPermissions.getSharedInstance(getApplicationContext()).updatedPreferences();
                    return true;
                }

                return false;
            }
        };


        ListPreference encryption_list_pref = (ListPreference)findPreference(getString(R.string.BB_PREFERENCE_ENCRYPTION_LEVEL));

        CheckBoxPreference researcher_pref = (CheckBoxPreference)findPreference(getString(R.string.BB_PREFERENCE_FLAG_RESEARCHER_SESSION));
        CheckBoxPreference log_io_pref = (CheckBoxPreference)findPreference(getString(R.string.BB_PREFERENCE_LOGIO));

        Preference sync_pref = findPreference(getString(R.string.BB_PREFERENCE_FORCE_SYNC));
        Preference encrypt_pref = findPreference(getString(R.string.BB_PREFERENCE_FORCE_ENCRYPT));
        Preference app_permissions = findPreference(getString(R.string.BB_PREFERENCE_APP_PERMISSIONS));

        encryption_list_pref.setOnPreferenceChangeListener(preferenceChangeListener);
        researcher_pref.setOnPreferenceChangeListener(preferenceChangeListener);
        log_io_pref.setOnPreferenceChangeListener(preferenceChangeListener);

        sync_pref.setOnPreferenceClickListener(preferenceClickListener);
        encrypt_pref.setOnPreferenceClickListener(preferenceClickListener);
        app_permissions.setOnPreferenceClickListener(preferenceClickListener);

    }
}
