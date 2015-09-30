package tbb.core.logger;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import blackbox.external.logger.BaseLogger;
import blackbox.external.logger.Logger;

/**
 * Created by kylemontague on 29/12/14.
 */
public class MessageLogger extends Logger {

    private static final String _Name = "MessageLogger";
    private static MessageLogger mSharedInstance;

    public static MessageLogger sharedInstance(){
        if(mSharedInstance == null)
            mSharedInstance = new MessageLogger();
        return mSharedInstance;
    }


    protected MessageLogger(){
        super(_Name,2);

    }

    /**
     * Make a request to TBB for the current sequence folder location.
     */
    public void requestStorageInfo(Context context){
        Intent i= new Intent();
        i.setAction(BaseLogger.ACTION_SEND_REQUEST);
        i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(i);
        Log.v(BaseLogger.TAG, _Name+": Requested Storage Location");
    }

}
