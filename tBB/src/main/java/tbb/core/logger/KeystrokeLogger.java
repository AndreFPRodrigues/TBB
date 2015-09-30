package tbb.core.logger;

import android.util.Log;

import blackbox.external.logger.Logger;
import tbb.core.service.TBBService;
import tbb.interfaces.KeystrokeEventReceiver;


public class KeystrokeLogger extends Logger implements KeystrokeEventReceiver {

    private final static String SUBTAG = "KeystrokeLogger: ";

    public KeystrokeLogger(String name, int flushThreshold){
        super(name, flushThreshold);
    }

    /**
     * Write the string record into the log file
     */
    public void onKeystroke(String keystroke) {
       // Log.v(TBBService.TAG, SUBTAG + "key: " + keystroke);
        writeAsync(keystroke);
    }
}
