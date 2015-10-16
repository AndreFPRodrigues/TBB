package blackbox.external.logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;



/**
 * Created by kyle montague, and Hugo Nicolau (he sat behind me shouting that I was wrong the whole time.) on 10/11/2014.
 */
public class Logger implements BaseLogger{



    private static final String SUBTAG = "Logger: ";
    protected final Object mLock = new Object(); // used to synchronize access to mData

    protected String mFolderName = "";
    protected String mFilename = "";
    protected String mSequence;
    protected String mName;
    protected ArrayList<String> mData = null;

    private Context mContext;

    private BroadcastReceiver mStorageReceiver = null;

    protected int mFlushThreshold = 1000;
    protected final int MIN_FLUSH_THRESHOLD = 50;

    protected Logger(String name, int flushThreshold) {

        // initialize variables
        mName = name;
        mFlushThreshold = Math.max(MIN_FLUSH_THRESHOLD, flushThreshold);
        mData = new ArrayList<String>();
    }

    public final static IntentFilter INTENT_FILTER;
    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(BaseLogger.ACTION_UPDATE);
        INTENT_FILTER.addAction(BaseLogger.ACTION_FLUSH);
        INTENT_FILTER.addAction(BaseLogger.ACTION_STOP);
        INTENT_FILTER.addAction(BaseLogger.ACTION_LOCATION);
    }

    @Override
    public void start(Context context) {
        // configure broadcast receiver
        mContext = context;
        mStorageReceiver = new StorageReceiver();
        mContext.registerReceiver(mStorageReceiver,
                Logger.INTENT_FILTER);
    }

    @Override
    public void stop() {
    	
        if(mContext != null) {
            mContext.unregisterReceiver(mStorageReceiver);
            mContext = null;
        }
        flush();
    }

    public void onStorageUpdate(String path, String sequence){
        Log.v(BaseLogger.TAG, SUBTAG + "onStorageUpdate:"+path+" sequence:"+sequence);
        setFileInfo(path, sequence);
    }

    public void onLocationReceived(String path, String sequence) {
        Log.v(BaseLogger.TAG, SUBTAG + "onLocationReceived:" + path + " sequence:" + sequence);
        if(mSequence!= sequence)
            setFileInfo(path,sequence);
    }

    public void onFlush(){
        //Log.v(BaseLogger.TAG, SUBTAG + "onFlush");
        flush();
    }

    private void setFileInfo(String path, String sequence){
        //Log.v(BaseLogger.TAG, SUBTAG + "SetFIleInfo: "+path);
        mFolderName = path+"/"+mName;
        mSequence = sequence;
        mFilename = mFolderName+"/"+mSequence+"_"+mName+".txt";
    }

    private void flush(){
        //Log.v(BaseLogger.TAG, SUBTAG + "Flush - "+mData.size()+" file: "+mFilename);
        DataWriter w = new DataWriter(mFolderName, mFilename, true);
        synchronized (mLock) {
            w.execute(mData.toArray(new String[mData.size()])); // data is passed to background thread
            mData = new ArrayList<String>(); // initialization
        }
    } 

    public void writeAsync(String data){
        synchronized (mLock) {
            mData.add(data);
        }

        //Log.v(BaseLogger.TAG, SUBTAG + "mData size:"+mData.size());
        if (mData.size() >= mFlushThreshold)
            flush();
    }

    /**
     * StorageReceiver is responsible for catching broad casted StorageCoordinator
     * events.
     *
     * It extracts folder path and sequence number and updates local storage folder.
     */
    public class StorageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(BaseLogger.ACTION_UPDATE)){
                Bundle extras = intent.getExtras();
                String path = extras.getString(BaseLogger.EXTRAS_FOLDER_PATH);
                String sequence = extras.getString(BaseLogger.EXTRAS_SEQUENCE);

                onStorageUpdate(path, sequence);
            }
            else if(intent.getAction().equals(BaseLogger.ACTION_FLUSH)){
                onFlush();
            }
            else if(intent.getAction().equals(BaseLogger.ACTION_STOP)){
                stop();
            }else if(intent.getAction().toString().equals(BaseLogger.ACTION_LOCATION)){
                Bundle extras = intent.getExtras();
                String path = extras.getString(BaseLogger.EXTRAS_FOLDER_PATH);
                String sequence = extras.getString(BaseLogger.EXTRAS_SEQUENCE);
                onLocationReceived(path,sequence);
            }
        }


    }

}
