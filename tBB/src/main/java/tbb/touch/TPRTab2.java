package tbb.touch;

import tbb.core.CoreController;



import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Process touch event received, identify type of touch
 * 
 * Feed all the touch positions via store(), when it returns true a type of
 * touch has finished. Use identifyTouch() to get the type of touch iden
 * 
 * @author Andre Rodrigues
 * 
 */
public class TPRTab2 extends TouchRecognizer {

	private final String LT = "Tab2";
	private int slot = 0;

	/**
	 * Identify type of touch (slide/touch/long press)
	 * 
	 * @return
	 */

	private int identifyTouch() {
		double distance = 0;
		if (touches.size() < 5) {
			touches.clear();
			if ((longTouchTime - lastTouch) < -LONGPRESS_THRESHOLD)
				return LONGPRESS;
			return TOUCHED;
		} else {
			for (int i = 1; i < touches.size(); i++) {
				distance = Math.sqrt((Math.pow((touches.get(i).getY() - touches
						.get(i - 1).getY()), 2) + Math.pow((touches.get(i)
						.getX() - touches.get(i - 1).getX()), 2)));
				if (distance > 50) {
					slideXorigin = touches.get(0).getX();
					slideYorigin = touches.get(0).getY();
					touches.clear();
					return SLIDE;
				}
			}
			touches.clear();
			return TOUCHED;
		}
	}

	/**
	 * Register in array all touch positions until finger release Detects single
	 * touch
	 * 
	 * @return (-1) -> no touch identified yet (0) -> touch identified (1) ->
	 *         slide identified (2) -> LongPress identified
	 */
	@Override
	public int identifyOnRelease(int type, int code, int value, int timestamp) {
		// + timestamp);
		if (code == ABS_MT_TRACKING_ID) {
			identifier = value;
		} else if (code == ABS_MT_PRESSURE)
			pressure = value;
		else if (code == ABS_MT_TOUCH_MAJOR)
			touchMajor = value;
		else if (code == ABS_MT_TOUCH_MINOR)
			touchMinor = value;
		if (code == ABS_MT_POSITION_X) {
			lastX = value;
		} else if (code == ABS_MT_POSITION_Y)
			lastY = value;
		else if (code == SYN_REPORT && value == 0
				&& lastEventCode != ABS_MT_TRACKING_ID) {
			TouchEvent p = new TouchEvent(lastX, lastY, timestamp, pressure,
					touchMajor, touchMinor, identifier);
			longTouchTime = timestamp;
			touches.add(p);
		}
		if (code == ABS_MT_TRACKING_ID && value == -1
				&& (lastEventCode == SYN_REPORT) && touches.size() > 0) {
			lastEventCode = -1;
			// prevents double tap
			if ((lastTouch - timestamp) < -doubleTapThreshold) {

				lastTouch = timestamp;

				return identifyTouch();
			} else
				return -1;
		}
		lastEventCode = code;
		return -1;
	}

	/**
	 * Register in array all touch positions until finger release
	 * 
	 * @return (-1) -> no touch identified yet (0) -> down identified (1) ->
	 *         move identified (2) -> up identified
	 */
	@Override
	public int identifyOnChange(int type, int code, int value, int timestamp) {
		//Log.d(LT, "t:" + type + " c:" + code + " v:" + value); // " time:"

		switch (code) {
		case ABS_MT_POSITION_X:
			// Log.d(LT, "X: " + value ); //" time:"
			xs.put(slot, value);
			break;
		case ABS_MT_POSITION_Y:
			ys.put(slot, value);
			break;
		case ABS_MT_PRESSURE:
			pressure = value;
			break;
		case ABS_MT_TOUCH_MAJOR:
			touchMajor = value;
			break;
		case ABS_MT_TOUCH_MINOR:
			touchMinor = value;
			break;
		case ABS_MT_TRACKING_ID:
			// Log.d(LT, "ID: " + value ); //" time:"
			identifier = value;
			if (identifier > 0) {

				tou.remove(ids.get(slot));
				ids.put(slot, identifier);

			}

			break;
		case ABS_MT_SLOT:
			// Log.d(LT, "SLOT: " + value ); //" time:"
			slot = value;
			if (ids.containsKey(slot))
				identifier = ids.get(slot);
			break;
		case SYN_REPORT:

			if (identifier > 0) {
				if (tou.containsKey(ids.get(slot))) {
					tou.put(ids.get(slot),
							new TouchEvent(xs.get(slot), ys.get(slot),
									timestamp, pressure, touchMajor,
									touchMinor, ids.get(slot)));

					return MOVE;
				}
				tou.put(ids.get(slot),
						new TouchEvent(xs.get(slot), ys.get(slot), timestamp,
								pressure, touchMajor, touchMinor, ids.get(slot)));

				return DOWN;
			} else if (identifier == -1) {
				if (ids.containsKey(slot)) {
					tou.put(ids.get(slot),
							new TouchEvent(xs.get(slot), ys.get(slot),
									timestamp, pressure, touchMajor,
									touchMinor, ids.get(slot)));

					return UP;
				}
			}

		}

		// Log.d(LT, "t:" + type + " c:" + code + " v:" + value);
		return -1;
	}

	private void checkResetTouches() {
		boolean reset = true;
		for (int i = 0; i < id_touches.size(); i++) {
			if (id_touches.get(i) != -1) {
				reset = false;
				break;
			}
		}
		if (reset)
			id_touches.clear();
	}

	private void clearTouchesFromId() {
		int aux = -1;
		for (int i : id_touches) {
			aux++;
			if (i == -1) {
				id_touches.remove(aux);
				break;
			}
		}
	}

	@Override
	public int getIdentifier() {
		if (identifier < 0)
			return -identifier;
		return identifier;
	}

	@Override
	public TouchEvent getlastTouch() {

		return tou.get(ids.get(slot));
	}
}
