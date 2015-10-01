package tbb.core;

import java.util.ArrayList;

import tbb.core.logger.IOTreeLogger;
import tbb.core.logger.KeystrokeLogger;
import tbb.core.logger.MessageLogger;
import tbb.core.service.TBBService;
import tbb.core.ioManager.Monitor;
import tbb.interfaces.AccessibilityEventReceiver;
import tbb.interfaces.IOEventReceiver; 
import tbb.interfaces.NotificationReceiver;
import tbb.touch.TouchRecognizer;


import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
 
import android.preference.PreferenceManager;   
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class CoreController {

	// debugging tag
	private final static String SUBTAG = "CoreController: ";

    public static final String ACTION_INIT = "BB.ACTION.CORECONTROLLER.INIT";
    public static final String ACTION_STOP = "BB.ACTION.CORECONTROLLER.STOP";

    // singleton instance
    private static CoreController mSharedInstance = null;

	// List of receivers 
	private ArrayList<IOEventReceiver> mIOEventReceivers;
	private ArrayList<NotificationReceiver> mNotificationReceivers;
	private ArrayList<AccessibilityEventReceiver> mAccessibilityEventReceivers;
	private ArrayList<KeystrokeLogger> mKeystrokeEventReceiver;

    // module where to forward messages
    private Monitor mMonitor;

	// touch recognizer
	private TouchRecognizer mTouchRecognizer = null;


    // message logger
    private MessageLogger mMessageLogger = null;


	// context, a.k.a tbb service
	private TBBService mTBBService = null;

	// IO Variables
	public static final int SET_BLOCK = 0;
	public static final int MONITOR_DEV = 1;
	public static final int CREATE_VIRTUAL_TOUCH = 2;
	public static final int SETUP_TOUCH = 3;
	public static final int SET_TOUCH_RAW = 4;
	public static final int FOWARD_TO_VIRTUAL = 5;
 
	// Mapped screen resolution
	public double M_WIDTH;
	public double M_HEIGHT;

	public boolean permission=true;

    protected CoreController() {}

    public synchronized static CoreController sharedInstance() {
        if(mSharedInstance == null) mSharedInstance = new CoreController();
        return mSharedInstance;
    }

    public TBBService getTBBService(){
        return mTBBService;
    }

	/**
	 * Initialise CoreController
	 */
	public void initialize(Monitor monitor, TBBService tbbService) {
        Log.v(TBBService.TAG, SUBTAG + "initialize");
		mMonitor = monitor;
		mTBBService = tbbService;


		// initialise receivers
        initializeReceivers();

		// get screen resolution
		configureScreen();

        // announce service start
        startService();
	}

    private void initializeReceivers(){

        // Notification receivers
        // TODO are we using notifications?
        mNotificationReceivers = new ArrayList<NotificationReceiver>();

        // Event Receivers
        mAccessibilityEventReceivers = new ArrayList<AccessibilityEventReceiver>();
        IOTreeLogger ioTreeLogger = new IOTreeLogger("IO", "Tree", 250, 50,"Interaction");
        ioTreeLogger.start(mTBBService.getApplicationContext());
        registerAccessibilityEventReceiver(ioTreeLogger);

        // IO receivers
        mIOEventReceivers = new ArrayList<IOEventReceiver>();
        registerIOEventReceiver(ioTreeLogger);

        // Logger receivers
        mKeystrokeEventReceiver = new ArrayList<KeystrokeLogger>();
        KeystrokeLogger ks = new KeystrokeLogger("Keystrokes", 150);
        ks.start(mTBBService.getApplicationContext());
        registerKeystrokeEventReceiver(ks);

        mMessageLogger = MessageLogger.sharedInstance();
        mMessageLogger.start(mTBBService.getApplicationContext());
    }

    private void configureScreen(){
        WindowManager wm = (WindowManager) mTBBService.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        Point size = new Point();
        display.getSize(size);
        M_WIDTH = size.x;
        M_HEIGHT = size.y;
    }
	/***********************************
	 * IO Commands and messages
	 * 
	 ************************************* */

	/**
	 * Register logger receiver (receives keystrokes info)
	 */
	public boolean registerKeystrokeEventReceiver(KeystrokeLogger receiver) {
        return mKeystrokeEventReceiver != null ? mKeystrokeEventReceiver.add(receiver) : false;
	}


    public boolean unregisterKeystrokeEventReceiver(KeystrokeLogger receiver) {
        return mKeystrokeEventReceiver != null ? mKeystrokeEventReceiver.remove(receiver) : false;
    }

    /**
     * Keystrokes propagated to mKeystrokeEventReceiver
     */
    public void updateKeystrokeEventReceivers(String keystroke, long timestamp, String text) {

        if(mKeystrokeEventReceiver == null) return;
        for(KeystrokeLogger receiver: mKeystrokeEventReceiver){
            receiver.onKeystroke(keystroke, timestamp, text);
        }
	}

	/**
	 * Register IO events receiver
	 */
	public boolean registerIOEventReceiver(IOEventReceiver ioReceiver) {
        return mIOEventReceivers != null ? mIOEventReceivers.add(ioReceiver) : false;
	}

	/**
	 * Unregister IOReceiver
	 */
	public boolean unregisterIOReceiver(IOEventReceiver ioReceiver) {
        return mIOEventReceivers != null ? mIOEventReceivers.remove(ioReceiver) : false;
	}

	/**
	 * IO event propagated to IOReceivers
	 */
	public void updateIOReceivers(int device, int type, int code, int value, int timestamp) {

        if(mIOEventReceivers == null) return;
        for(IOEventReceiver receiver: mIOEventReceivers){
            receiver.onUpdateIOEvent(device, type, code, value,
					timestamp);
        }
	}

	public void sendTouchIOReceivers(int type) {

        if(mIOEventReceivers == null) return;
        for(IOEventReceiver receiver: mIOEventReceivers){
            receiver.onTouchReceived(type);
        }

	}

	/**
	 * Forwards the message to the appropriate component
	 * 
	 * @param command
	 *            - SET_BLOCK/MONITOR_DEV/CREATE_VIRTUAL_TOUCH/SETUP_TOUCH
	 * @param index
	 *            - device index for SET_BLOCK/MONITOR_DEV/SETUP_TOUCH
	 * @param state
	 *            - state SET_BLOCK/MONITOR_DEV
	 */
	public void commandIO(final int command, final int index,
			final boolean state) {

		Thread b = new Thread(new Runnable() {
			public void run() {

				// Separates and forwards messages to the appropriate module
				switch (command) {
				case SET_BLOCK:
					mMonitor.setBlock(index, state);
					break;
				case MONITOR_DEV:
					mMonitor.monitorDevice(index, state);
					break;
				case CREATE_VIRTUAL_TOUCH:
					mMonitor.createVirtualTouchDrive(index);
					break;
				case SETUP_TOUCH:
					mTBBService.storeTouchIndex(index);
					mMonitor.setupTouch(index);
					break;

				}
			}
		});
		b.start();
	}

	/**
	 * Inject event into touch virtual drive
	 * 
	 * @requires virtual touch driver created
	 * @param t
	 *            type
	 * @param c
	 *            code
	 * @param v
	 *            value
	 */
	public void injectToVirtual(int t, int c, int v) {
        mMonitor.injectToVirtual(t, c, v);
	}

	public void injectToTouch(int t, int c, int v) {
        mMonitor.injectToTouch(t, c, v);
	}

	/**
	 * Inject event into the device on the position index
	 * 
	 * @param index
	 * @param type
	 * @param code
	 * @param value
	 */
	public void inject(int index, int type, int code, int value) {
		mMonitor.inject(index, type, code, value);
	}

	public int monitorTouch(boolean state) {
        return mMonitor.monitorTouch(state);
	}

	/**
	 * Get list of internal devices (touchscree, keypad, etc)
	 * 
	 * @return
	 */
	public String[] getDevices() {
        return mMonitor.getDevices();
	}

	/*************************************************
	 * Navigation and content Commands and messages
	 * 
	 ************************************************** 
	 **/

	/**
	 * Register events
	 *
	 */
	public boolean registerAccessibilityEventReceiver(AccessibilityEventReceiver eventReceiver) {

		return mAccessibilityEventReceivers != null ? mAccessibilityEventReceivers.add(eventReceiver) : false;
	}

	public boolean unregisterEvent(AccessibilityEventReceiver eventReceiver) {

        return mAccessibilityEventReceivers != null ? mAccessibilityEventReceivers.remove(eventReceiver) : false;
	}

	public void updateAccessibilityEventReceivers(AccessibilityEvent event) {

        if(mAccessibilityEventReceivers == null) return;
        for(AccessibilityEventReceiver receiver: mAccessibilityEventReceivers){
            if(checkEvent(receiver.getType(), event)) receiver.onUpdateAccessibilityEvent(event);
        }
	}

	private boolean checkEvent(int[] type, AccessibilityEvent event) {
		for (int i = 0; i < type.length; i++) {
			if (type[i] == event.getEventType())
				return true;
		}
		return false;
	}

	/*************************************************
	 * Auxiliary functions
	 * 
	 ************************************************** 
	 **/
	/**
	 * Calculate the mapped coordinate of x
	 * 
	 * @param x
	 * @return
	 */
	public int xToScreenCoord(double x) {

        return (int) (M_WIDTH / mTBBService.getScreenSize()[0] * x);
	}

	/**
	 * Calculate the mapped coordenate of y
	 * 
	 * @param
	 * @return
	 */
	public int yToScreenCoord(double y) {
		return (int) (M_HEIGHT / mTBBService.getScreenSize()[1] * y);
	}

	public void stopService() {

        // clear receivers
        mNotificationReceivers = null;
        mAccessibilityEventReceivers = null;
        mIOEventReceivers = null;
        mKeystrokeEventReceiver = null;


        if(mMonitor!=null)
            mMonitor.stop();

	}

	public void stopServiceNoBroadCast() {

        // clear receivers
//        mNotificationReceivers = null;
//        mAccessibilityEventReceivers = null;
//        mIOEventReceivers = null;
//        mKeystrokeEventReceiver = null;

        if(mMonitor!=null)
            mMonitor.stop();
	}

    public void startServiceNoBroadCast() {
        Log.v(TBBService.TAG,"STARTING MONITOR TOUCH");
        if(mMonitor!=null)
            mMonitor.monitorTouch(true);
    }

	private void startService() {
        Log.d(TBBService.TAG, SUBTAG + "starting service");

		// Broadcast init event
		Intent intent = new Intent();
		intent.setAction(ACTION_INIT);
		mTBBService.sendBroadcast(intent);
        //mTBBService.registerReceiver(IOTreeLogger.sharedInstance(),IOTreeLogger.INTENT_FILTER);
        //CoreController.registerLogger(IOTreeLogger.sharedInstance(mTBBService.getApplicationContext()));
	}

	public void setScreenSize(int width, int height) {

		mTBBService.storeScreenSize(width, height);
	}

	/**
	 * Returns to home
	 * 
	 * @return
	 */
	public boolean home() {

        return mTBBService.home();
	}

	/**
	 * Returns to home
	 */
	public boolean back() {

        return mTBBService.back();
	}

	/**
	 * Register a notification receiver
	 * 
	 * @param nr
	 * @return
	 */
	public int registerNotificationReceiver(NotificationReceiver nr) {
		mNotificationReceivers.add(nr);
		return mNotificationReceivers.size() - 1;
	}

	public int getNotificationReceiversSize() {

        return mNotificationReceivers.size();
	}

	/**
	 * Update all notifications receivers
	 * 
	 * @param note
	 */
	public void updateNotificationReceivers(String note) {
		int size = mNotificationReceivers.size();
		if (note.equals("[]")) {
			return;
		}

		note = note.substring(1, note.length() - 1);

		for (int i = 0; i < size; i++) {
			mNotificationReceivers.get(i).onNotification(note);
		}

	}

	public void registerActivateTouch(TouchRecognizer touchRecognizer) {

		mTouchRecognizer = touchRecognizer;
	}

	public TouchRecognizer getActiveTPR() {

        return mTouchRecognizer;
	}

	// Screen reader function convert pixels to milimeter for android nexus s
	public static int convertToMilY(int y) {
		return (124 * y) / 800 * 5;
	}

	// Screen reader function convert pixels to milimeter for android nexus s
	public static int convertToMilX(int x) {
		return (63 * x) / 480 * 5;
	}

	public static double distanceBetween(double x, double y, double x1,
			double y1) {
		return Math.sqrt(Math.pow(y - y1, 2) + Math.pow(x - x1, 2));
	}

	public int currentFileId() {
		return PreferenceManager.getDefaultSharedPreferences(mTBBService).getInt(
				"preFileSeq", 0);
	}


}
