package tbb.core.logger;

import blackbox.external.logger.Logger;
import tbb.interfaces.KeystrokeEventReceiver;


public class KeystrokeLogger extends Logger implements KeystrokeEventReceiver {

    private final static String SUBTAG = "KeystrokeLogger: ";

    public KeystrokeLogger(String name, int flushThreshold){
        super(name, flushThreshold);
    }

    /**
     * Write the string record into the log file
     */
    public void onKeystroke(String keystroke, long timestamp, String text) {

       String json = "{\"keystroke\":\"" + keystroke + "\"" +
                        " , \"timestamp\":" + timestamp +
                        " , \"text\":\"" + text +
                        "\"},";
        writeAsync(json);
    }
}
