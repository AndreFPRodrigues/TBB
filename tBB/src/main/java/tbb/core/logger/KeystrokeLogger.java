package tbb.core.logger;

import blackbox.external.logger.Logger;
import tbb.interfaces.KeystrokeEventReceiver;


public class KeystrokeLogger extends Logger implements KeystrokeEventReceiver {

    private final static String SUBTAG = "KeystrokeLogger: ";
    //threshold on text size of the log to prevent full documents being recorded at every keystroke.
    //TODO give the option to save the full text box always or never
    public final static int TEXT_SIZE_THRESHOLD=100;
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
