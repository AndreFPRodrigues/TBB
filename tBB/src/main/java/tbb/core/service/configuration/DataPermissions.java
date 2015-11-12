package tbb.core.service.configuration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import blackbox.external.logger.DataWriter;
import blackbox.tinyblackbox.R;

/**
 * Created by Kyle Montague on 14/10/15.
 */
public class DataPermissions {

    public enum type{
        DO_NOT_LOG,
        ENCRYPT_ALL,
        ENCRYPT_SENSITIVE,
        ENCRYPT_NOTHING
    }


    private static DataPermissions sharedInstance;
    public static DataPermissions getSharedInstance(Context context){
        if(sharedInstance == null)
            sharedInstance = new DataPermissions(context);
        return sharedInstance;
    }

    Context mContext;
    HashMap<String, Boolean> mPermissions;
    SharedPreferences mSharedPreferences;
    type mDefault = null;


    private DataPermissions(Context context){
        mContext = context;
        loadPermissions();
        loadPreferences();
    }

    public type logMode(String packageName){
        if (mPermissions.get(packageName) !=null){
            if(!mPermissions.get(packageName))
                return type.DO_NOT_LOG;
        }
        return loggingMode();
    }


    public boolean shouldLog(String packageName){
        if (mPermissions.get(packageName) !=null && isLogging()){
           return mPermissions.get(packageName);
        }
        return isLogging();
    }

    private boolean isLogging(){
        return (loggingMode() != type.DO_NOT_LOG);
    }

    public boolean touchLogging(){
        return mSharedPreferences.getBoolean(mContext.getString(R.string.BB_PREFERENCE_LOGIO), true);
    }

    public type loggingMode(){
        if(mDefault == null) {
            String modeID = mSharedPreferences.getString(mContext.getString(R.string.BB_PREFERENCE_ENCRYPTION_LEVEL), "" + type.ENCRYPT_SENSITIVE.ordinal());
            mDefault= type.values()[Integer.valueOf(modeID)];
        }
        return mDefault;
    }

    private void loadPreferences(){
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    /**
     * Tell DataPermissions that the user has changed their permission settings, refresh the variable from the shared preferences.
     */
    public void updatedPreferences(){
        mDefault = null;
    }

    private String filename;
    private void loadPermissions(){
        String configFolder = mContext.getString(R.string.ConfigurationFolder);
        String permissionsFile =  mContext.getString(R.string.PermissionsFileName);
        String appName = mContext.getString(R.string.app_name);

        filename = Environment.getExternalStorageDirectory()+"/"+appName
                +"/"+configFolder+"/"+permissionsFile+".csv";
        File textFile = new File(filename);
        mPermissions = new HashMap<>();
        if(textFile.exists()){
            InputStream is = null;
            try {
                is = new FileInputStream(filename);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] RowData = line.split(",");
                    if(RowData.length > 1){
                        mPermissions.put(RowData[0], Boolean.valueOf(RowData[1]));
                    }
                }
            }
            catch (IOException ex) {
                // handle exception
            }
            finally {
                try {
                    assert is != null;
                    is.close();
                }
                catch (IOException e) {
                    // handle exception
                }
            }
        }
    }

    public void setPermission(String application, boolean mode){
        mPermissions.put(application, mode);
    }

    public void savePermissions(){
        Object[] keys = mPermissions.keySet().toArray();
        if(keys.length > 0) {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            DataWriter writer = new DataWriter(file.getParent(), file.getAbsolutePath(), false);
            String[] data = new String[keys.length];
            for (int x = 0; x < data.length; x++) {
                data[x] = keys[x].toString() + "," + mPermissions.get(keys[x].toString());
            }
            writer.execute(data);
        }


    }


}
