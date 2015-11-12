package tbb.core.logger;

import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;

import blackbox.external.logger.DataWriter;
import blackbox.external.logger.Logger;
import tbb.core.CoreController;
import tbb.core.service.TBBService;
import tbb.interfaces.AccessibilityEventReceiver;
import tbb.interfaces.IOEventReceiver;
import tbb.touch.TouchEvent;
import tbb.touch.TouchRecognizer;

/**
 * Created by kylemontague on 11/11/2014.
 */
public class IOTreeLogger extends Logger implements AccessibilityEventReceiver,
        IOEventReceiver {
    private static final String SUBTAG = "IOTreeLogger: ";
    private static String mIOName;
    private static int mIOThreshold;
    private static String mIntName;
    private static int mIntThreshold;
    // EXTENDING TO LOG TWO COUPLED LOGGERS
    private final Object mIOLock = new Object(); // used to synchronize access to mIOData
    // interaction log
    private final Object mIntLock = new Object(); // used to synchronize access to mIOData
    private final int TIME_DELAY = 200;
    private int lastRead = -1;
    private int id = 0;
    private boolean isAdding = false;
    private String mTreeData;
    private ArrayList<String> mIOData;
    private String mIOFilename;
    private ArrayList<String> mIntData;
    private String mIntFilename;
    private TouchRecognizer mTPR;
    private int mTouchDevice;
    private long mTimestamp = 0;
    private long mLastUpdate = 0;

    public IOTreeLogger(String IOName, String treeName, int ioFlushThreshold,
                        int treeFlushThreshold, String interactionName) {
        super(treeName, treeFlushThreshold);
        // Log.v(TBBService.TAG, SUBTAG + "created");

        // configure new logger parameters
        mIOData = new ArrayList<String>();
        mIOName = IOName;
        mIOThreshold = ioFlushThreshold;

        // parameters of interaction log
        mIntData = new ArrayList<String>();
        mIntName = interactionName;
        mIntThreshold = ioFlushThreshold;

        mTPR = CoreController.sharedInstance().getActiveTPR();
        mTouchDevice = CoreController.sharedInstance().monitorTouch(true);

    }

    @Override
    public void stop() {
        try {
            super.stop();

            // stops monitoring device
            CoreController.sharedInstance().commandIO(
                    CoreController.MONITOR_DEV, mTouchDevice, false);

            flushIO();
            flushInteraction();

        } catch (Exception e) {
            Toast.makeText(CoreController.sharedInstance().getTBBService(),
                    "TBB Exception", Toast.LENGTH_LONG).show();
            TBBService.writeToErrorLog(e);
        }
    }

    @Override
    public void onStorageUpdate(String path, String sequence) {
        try {
            super.onStorageUpdate(path, sequence);
            // Log.v(TBBService.TAG, SUBTAG + "onStorageUpdate");
            setIOFileInfo(path, sequence);
            setInteractionFileInfo(path, sequence);
        } catch (Exception e) {
            Toast.makeText(CoreController.sharedInstance().getTBBService(),
                    "TBB Exception", Toast.LENGTH_LONG).show();
            TBBService.writeToErrorLog(e);
        }
    }

    @Override
    public void onFlush() {
        try {
            super.onFlush();
            // Log.v(TBBService.TAG, SUBTAG + "onFlush");
            flushIO();
            flushInteraction();
        } catch (Exception e) {
            Toast.makeText(CoreController.sharedInstance().getTBBService(),
                    "TBB Exception", Toast.LENGTH_LONG).show();
            TBBService.writeToErrorLog(e);
        }
    }

    private void flushIO() {
        // Log.v(TBBService.TAG, SUBTAG +
        // "FlushIO - "+mIOData.size()+" file: "+mIOFilename);
        //synchronized (mIOLock) {
            DataWriter w = new DataWriter(mFolderName, mIOFilename, true);
            w.execute(mIOData.toArray(new String[mIOData.size()]));
            mIOData = new ArrayList<String>();
        //}
    }

    private void setIOFileInfo(String path, String sequence) {
        // Log.v(TBBService.TAG, SUBTAG + "SetIOFileInfo: "+path);
        mIOFilename = mFolderName + "/" + mSequence + "_" + mIOName + ".json";
    }

    private void setInteractionFileInfo(String path, String sequence) {
        // Log.v(TBBService.TAG, SUBTAG + "SetIOFileInfo: "+path);
        mIntFilename = mFolderName + "/" + mSequence + "_" + mIntName + ".json";
    }

    public void writeIOAsync(String data) {

       // synchronized (mIOLock) {
            mIOData.add(data);
       // }

        // Log.v(TBBService.TAG, SUBTAG + "mIOData size:"+mIOData.size());
        if (mIOData.size() >= mIOThreshold)
            flushIO();

    }

    /**
     * LOGGER RECEIVERS
     */

    @Override
    public void onUpdateAccessibilityEvent(AccessibilityEvent event) {

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED
                || event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
            interactionLog(event);
            return;
        }

        if (event.getEventTime() < mTimestamp) {
            return;
        }

        AccessibilityNodeInfo src = event.getSource();
        AccessibilityNodeInfo parent = AccessibilityScrapping
                .getRootParent(src);
        if (parent == null || src == null) {
            return;
        }


        Tree current = new Tree(AccessibilityEvent.eventTypeToString(event.getEventType()),
                AccessibilityScrapping.getEventText(event),
                System.currentTimeMillis(), AccessibilityScrapping
                .getCurrentActivityName(CoreController.sharedInstance()
                        .getTBBService()), id);
        current.setTreeStructure(parent);

        if (current.toString().hashCode() != lastRead || lastRead == -1) {
            id++;
            synchronized (mLock) {
                mLastUpdate = System.currentTimeMillis();
                mTreeData = current.toString();
            }

            if (!isAdding) {
                isAdding = true;
                queueTreeForUpdate();
            }
            lastRead = current.toString().hashCode();

            //Only log tree if there is no event in the next 200ms

        }

    }

    private void queueTreeForUpdate() {
        Handler toAdd = new Handler();
        toAdd.postDelayed(new Runnable() {
            public void run() {
                long time =System.currentTimeMillis();
                long timeToCompare=0;
                String dataCopy = null;
                synchronized (mLock) {
                     timeToCompare=mLastUpdate;
                     dataCopy = new String(mTreeData);
                    mLastUpdate+=TIME_DELAY;
                }
                if (timeToCompare + TIME_DELAY < time) {
                    mLastUpdate=mLastUpdate+TIME_DELAY;
                    //synchronized (mLock) {
                        if(isAdding)
                            writeAsync(dataCopy);

                        isAdding = false;
                    //}

                } else {
                    queueTreeForUpdate();
                }
            }
        }, TIME_DELAY);
    }


    private void interactionLog(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        String eventType = AccessibilityEvent.eventTypeToString(event.getEventType());
        String app = event.getPackageName().toString();
        String step = AccessibilityScrapping.getEventText(event);
        Log.d(TBBService.TAG, "Interaction:" + step);

        source = event.getSource();
        if (source == null) {
            Log.d(TBBService.TAG, "Interaction:" + step + " (source=null)");
            writeInteractionAsync("{\"treeID\":" + id +
                    " , \"desc\":\"" + step + "\"" +
                    " , \"timestamp\":" + System.currentTimeMillis() +
                    " , \"package\":\"" + app + "\"" +
                    " , \"type\":\"" + eventType +
                    "\" },");
            return;
        }

        String sourceText = AccessibilityScrapping.getDescriptionNew(source);
        if (step.length() < 1) {
            Log.d(TBBService.TAG, "Interaction:" + step + " (stepLength<1)");
            step = sourceText;
        }
        writeInteractionAsync("{\"treeID\":" + id +
                " , \"desc\":\"" + step + "\"" +
                " , \"timestamp\":" + System.currentTimeMillis() +
                " , \"app\":\"" + app + "\"" +
                " , \"type\":\"" + eventType + "\" },");

    }

    public void writeInteractionAsync(String data) {

       // synchronized (mIntLock) {
            mIntData.add(data);
       // }

        // Log.v(TBBService.TAG, SUBTAG + "mIOData size:"+mIOData.size());
        if (mIntData.size() >= mIntThreshold)
            flushInteraction();

    }

    private void flushInteraction() {

        Log.v(TBBService.TAG, SUBTAG +
                "FlushIO - " + mIntData.size() + " file: " + mIntFilename);
       // synchronized (mIntLock) {
            DataWriter w = new DataWriter(mFolderName, mIntFilename, true);
            w.execute(mIntData.toArray(new String[mIntData.size()]));
            mIntData = new ArrayList<String>();
       // }
    }

    @Override
    public int[] getType() {
        int[] type = new int[5];
        type[0] = AccessibilityEvent.TYPE_VIEW_CLICKED;
        type[1] = AccessibilityEvent.TYPE_VIEW_SCROLLED;
        type[2] = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        type[3] = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        type[4] = AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
        return type;
    }

    @Override
    public void onUpdateIOEvent(int device, int type, int code, int value,
                                int timestamp, long sysTime) {

        if (mTouchDevice == device) {
            int touchType;
            if ((touchType = mTPR
                    .identifyOnChange(type, code, value, timestamp)) != -1) {

                TouchEvent te = mTPR.getlastTouch();
                mTimestamp = timestamp;
                String json = "{\"treeID\":" + id +
                        " , \"dev\":" + device +
                        " , \"type\":" + touchType +
                        " , \"id\":" + te.getIdentifier() +
                        " , \"x\":" + te.getX() +
                        " , \"y\":" + te.getY() +
                        " , \"pressure\":" + te.getPressure() +
                        " , \"devTime\":" + te.getTime() +
                        " , \"timestamp\":" + sysTime +
                        "},";
                writeIOAsync(json);
            }
        } else {
            if (type != 0) {
                String json = "{\"treeID\":" + id +
                        " , \"dev\":" + device +
                        " , \"type\":" + type +
                        " , \"code\":" + code +
                        " , \"value\":" + value +
                        " , \"devTime\":" + timestamp +
                        " , \"timestamp\":" + sysTime +
                        "},";
                writeIOAsync(json);
            }
        }
    }

    @Override
    public void onTouchReceived(int type) {
    }
}
