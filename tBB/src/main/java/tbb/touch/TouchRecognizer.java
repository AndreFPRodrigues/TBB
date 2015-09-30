package tbb.touch;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;

public abstract class TouchRecognizer {
	
		// Touch types on release
		public final static int TOUCHED = 5;
		public final static int SLIDE = 6;
		public final static int LONGPRESS = 7;

		// Touch types on change
		public final static int DOWN = 0;
		public final static int MOVE = 1;
		public final static int UP = 2;
		public final static int DOUBLE_CLICK = 3;
		public final static int SPLIT_TAP = 4;

		// input types
		public static final int ABS_MT_POSITION_X = 53;
		public static final int ABS_MT_POSITION_Y = 54;
		public static final int ABS_MT_PRESSURE = 58;
		public static final int ABS_MT_TOUCH_MAJOR = 48;
		public static final int ABS_MT_TOUCH_MINOR = 49;
		public static final int ABS_MT_TRACKING_ID = 57;
		public static final int SYN_MT_REPORT = 2;
		public static final int SYN_REPORT = 0;
		public static final int ABS_MT_SLOT = 47;

		// identifier variables
		HashMap<Integer, Integer> xs = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> ys = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> ids = new HashMap<Integer, Integer>();

		HashMap<Integer, TouchEvent> tou = new HashMap<Integer, TouchEvent>();
		protected int lastX;
		protected int lastY;
		protected int slideXorigin;
		protected int slideYorigin;
		protected int lastEventCode = -1;
		protected int pressure;
		protected int touchMajor;
		protected int touchMinor;
		protected int identifier;
		protected int time;
		
		// Array to store all touch events
		protected ArrayList<TouchEvent> touches = new ArrayList<TouchEvent>();

		// number of fingers used
		protected int numberTouches = 0;
		protected ArrayList<Integer> id_touches = new ArrayList<Integer>();
		protected int idFingerUp;
		protected int biggestIdentifier = 0;

		// used to prevent double tap
		protected int lastTouch = 0;
		protected int doubleTapThreshold = 500;

		// used to identify long touch
		protected int longTouchTime = 0;
		protected final int  LONGPRESS_THRESHOLD=400;
		
		
		/**
		 * Register in array all touch positions until finger release
		 * 
		 * @return (-1) -> no touch identified yet (0) -> down identified (1) ->
		 *         move identified (2) -> up identified
		 */
		public abstract int identifyOnChange(int type, int code, int value , int timestamp);


		/**
		 * Register in array all touch positions until finger release
		 * 
		 * @return (-1) -> no touch identified yet (0) -> touch identified (1) ->
		 *         slide identified (2) -> LongPress identified
		 */
		public abstract int identifyOnRelease(int type, int code, int value,
				int timestamp);
		
		public int getLastX() {
			return lastX;
		}

		public int getLastY() {
			return lastY;
		}

		public int getOriginX() {
			return slideXorigin;
		}

		public int getOriginY() {
			return slideXorigin;
		}

		public int getPressure() {
			return pressure;
		}
		public int getIdentifier() {
			return identifier;
		}
		public int getTouchSize() {
			return touchMajor;
		}
		public int getTimestamp() {
			return time;
		}
		public int getIdFingerUp() {
			return idFingerUp;
		}
		
		/**
		 * Check if distance between touch events is greater than 5px in any axis
		 * 
		 * @param p
		 * @param te
		 * @return
		 */
		protected boolean checkDistance(TouchEvent p, TouchEvent te) {

			if (Math.pow((p.getX() - te.getX()), 2) > 5)
				return true;
			if (Math.pow((p.getY() - te.getY()), 2) > 5)
				return true;
			return false;
		}
		
		/**
		 * Checks if the touch event moved more than 5px
		 * if not it returns false
		 * @param p
		 * @return
		 */
		protected boolean checkIfMoved(TouchEvent p) {
			TouchEvent te;
			for(int i=touches.size()-2;i>0;i--){
				if((te=touches.get(i)).getIdentifier()==p.getIdentifier())
					return checkDistance(p,te);
			}
			return false;
		}
		
		
		public TouchEvent getlastTouch() {

			return null;
		}
	
}
