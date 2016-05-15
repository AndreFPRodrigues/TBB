package tbb.touch;

import android.util.Log;

import java.util.Map;

/**
 * /**
 * Process touch event received, identify type of touch
 * Feed all the touch positions via store(), when it returns true a type of
 * touch has finished.
 * For devices capable of tracking identifiable contacts (type B)
 * for more on the multi-touch protocols
 * https://www.kernel.org/doc/Documentation/input/multi-touch-protocol.txt
 * @author Andre Rodrigues
 */
public class TPR_ProtocolB extends TouchRecognizer {

    private final String LT = "TPR_B";
    private int slot = 0, nextSlot = 0;
    private boolean wasUP = false;
    private int lastID = -1;
    private int lastTimestamp = -1;
    //private boolean multitouch = false;

    /**
     * Identify type of touch (slide/touch/long press)
     *
     * @return
     */

    private int identifyTouch(int identifier) {
        double distance = 0;
        if (touches.size() < 5) {
            touches.clear();
            if ((longTouchTime - lastTouch.get(identifier)) < -LONGPRESS_THRESHOLD)
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
     * slide identified (2) -> LongPress identified
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

            //check if identifier is in array, update value
            lastXs.put(identifier, value);

        } else if (code == ABS_MT_POSITION_Y) {
            //if it already contains identifier, itll just update the value
            lastYs.put(identifier, value);
        } else if (code == SYN_REPORT && value == 0
                && lastEventCode.get(identifier) != ABS_MT_TRACKING_ID) {
            TouchEvent p = new TouchEvent(lastXs.get(identifier), lastYs.get(identifier), timestamp, pressure,
                    touchMajor, touchMinor, identifier);
            longTouchTime = timestamp;
            touches.add(p);
        }
        if (code == ABS_MT_TRACKING_ID && value == -1
                && (lastEventCode.get(identifier) == SYN_REPORT) && touches.size() > 0) {
            lastEventCode.put(identifier, -1); //update or remove value at identifier
            // prevents double tap
            if ((lastTouch.get(identifier) - timestamp) < -doubleTapThreshold) {

                //check if identifier is in array, update value
                lastTouch.put(identifier, timestamp);

                return identifyTouch(identifier);
            } else
                return -1;
        }
        //check if identifier is in array, update value
        lastEventCode.put(identifier, code);
        return -1;
    }

    /*
        private void cleanTouches(){
            for (Map.Entry<Integer, TouchEvent> entry : tou.entrySet()) {
            //	Log.d(LT, "key: " + entry.getKey() + " value: "+entry.getValue().toString()); ;
                tou.remove(entry.getKey());
            }

        }
    */
    private void printMaps() {
        Log.d(LT, "PRINTING IDS");
        for (Map.Entry<Integer, Integer> entry : ids.entrySet()) {
            Log.d(LT, "key: " + entry.getKey() + " value: " + entry.getValue());
            ;
        }

        Log.d(LT, "PRINTING tou");
        for (Map.Entry<Integer, TouchEvent> entry : tou.entrySet()) {
            Log.d(LT, "key: " + entry.getKey() + " value: " + entry.getValue().toString());
            ;
        }
    }

    /**
     * Register in array all touch positions until finger release
     *
     * @return (-1) -> no touch identified yet (0) -> down identified (1) ->
     * move identified (2) -> up identified
     */
    @Override
    public int identifyOnChange(int type, int code, int value, int timestamp) {
        //Log.d(LT, "t:" + type + " c:" + code + " v:" + value); // " time:"
		/*if(lastTimestamp>-1){
			if(){}
		}*/

        switch (code) {
            case ABS_MT_POSITION_X:
                // Log.d(LT, "X: " + value ); //" time:"

                //xs.put(slot, value);
                x = value;
               // Log.d("TbbDatabaseHelper", "x is " + value + " when slot is " + slot);
                break;
            case ABS_MT_POSITION_Y:
                //ys.put(slot, value);
                y = value;
                //Log.d("TbbDatabaseHelper", "y is " + value + " when slot is " + slot);
                break;
            case ABS_MT_PRESSURE:
                pressure = value;
               // Log.d("TbbDatabaseHelper", "pressure is " + value + " when slot is " + slot);
                break;
            case ABS_MT_TOUCH_MAJOR:
                touchMajor = value;
                //Log.d("TbbDatabaseHelper", "touchMajor is " + value + " when slot is " + slot);
                break;
            case ABS_MT_TOUCH_MINOR:
                touchMinor = value;
                //Log.d("TbbDatabaseHelper", "touchMinor is " + value + " when slot is " + slot);
                break;
            case ABS_MT_TRACKING_ID:
               // Log.d("TbbDatabaseHelper", "Identifier is " + value + ".");
                identifier = value;
                if (identifier > lastID) {
                    lastID = identifier;
                }
                wasUP = false; //if we go through here its either an up or a down.

                break;
            case ABS_MT_SLOT:

               // Log.d("TbbDatabaseHelper", "Current slot is " + slot + ". Next slot is being attributed:" + value);

                //this wasnt here before ..
                if (wasUP && ids.get(slot) != null) {
                    identifier = ids.get(slot);
                    wasUP = false;
                }

                if (identifier > 0) {
                    if (!ids.containsValue(identifier)) {
                        tou.clear();

                        ids.put(slot, identifier);
                        coordinateCheck();

                       // Log.d("TbbDatabaseHelper", "ABS_MT_SLOT DOWN: identifier:" + identifier + " slot:" + slot +
                       //         " x:" + xs.get(slot) + " y:" + ys.get(slot) + " timestamp:" + timestamp + " pressure:" + pressure + " touchMajor:" + touchMajor +
                       //         " touchMinor:" + touchMinor + ".");

                        tou.put(ids.get(slot),
                                new TouchEvent(xs.get(slot), ys.get(slot), timestamp,
                                        pressure, touchMajor, touchMinor, ids.get(slot)));

                        //printMaps();

                        slot = value;

                        return DOWN;

                    } else if (ids.containsKey(slot)) {
                        tou.clear();

                        coordinateCheck();

                        tou.put(ids.get(slot),
                                new TouchEvent(xs.get(slot), ys.get(slot),
                                        timestamp, pressure, touchMajor,
                                        touchMinor, ids.get(slot)));
                       // printMaps();

                      //  Log.d("TbbDatabaseHelper", "ABS_MT_SLOT MOVE: identifier:" + ids.get(slot) + " slot:" + slot +
                      //          " x:" + xs.get(slot) + " y:" + ys.get(slot) + " timestamp:" + timestamp + " pressure:" + pressure + " touchMajor:" + touchMajor +
                      //          " touchMinor:" + touchMinor + ".");

                        slot = value;

                        return MOVE;

                    } else if (x > -1 && y > -1) { //is a DOWN but didnt receive identifier!
                        tou.clear();
                        identifier = lastID + 1;
                        lastID = identifier;

                        ids.put(slot, identifier);
                        coordinateCheck();

                      /*  Log.d("TbbDatabaseHelper", "ABS_MT_SLOT DOWN: identifier:" + identifier + " slot:" + slot +
                                " x:" + xs.get(slot) + " y:" + ys.get(slot) + " timestamp:" + timestamp + " pressure:" + pressure + " touchMajor:" + touchMajor +
                                " touchMinor:" + touchMinor + ".");*/

                        tou.put(ids.get(slot),
                                new TouchEvent(xs.get(slot), ys.get(slot), timestamp,
                                        pressure, touchMajor, touchMinor, ids.get(slot)));

                        //printMaps();

                        slot = value;

                        return DOWN;

                    }
                } else if (identifier == -1 && ids.get(slot) != null) {
                    tou.clear();

                    coordinateCheck();

                  /*  Log.d("TbbDatabaseHelper", "ABS_MT_SLOT UP: identifier:" + ids.get(slot) + " slot:" + slot +
                            " x:" + xs.get(slot) + " y:" + ys.get(slot) + " timestamp:" + timestamp + " pressure:" + pressure + " touchMajor:" + touchMajor +
                            " touchMinor:" + touchMinor + ".");*/

                    tou.put(ids.get(slot),
                            new TouchEvent(xs.get(slot), ys.get(slot),
                                    timestamp, pressure, touchMajor,
                                    touchMinor, ids.get(slot)));
                    //printMaps();
                    ids.remove(slot);
                    xs.remove(slot);
                    ys.remove(slot);

                    wasUP = true;
                    slot = value;

                    return UP;

                }
                slot = value;
                break;


            case SYN_REPORT:

               // Log.d("TbbDatabaseHelper", "SYN_REPORT. slot is " + slot + ". ids size is " + ids.size());

                if (wasUP && ids.get(slot) != null) {
                    identifier = ids.get(slot);
                    wasUP = false;
                }

                if (identifier > 0) {
                    if (!ids.containsValue(identifier)) {
                        tou.clear();

                        ids.put(slot, identifier);
                        coordinateCheck();

                       /* Log.d("TbbDatabaseHelper", "SYN_REPORT: DOWN identifier:" + ids.get(slot) + " slot:" + slot +
                                " x:" + xs.get(slot) + " y:" + ys.get(slot) + " timestamp:" + timestamp +
                                " pressure:" + pressure + " touchMajor:" + touchMajor +
                                " touchMinor:" + touchMinor + ".");*/

                        tou.put(ids.get(slot),
                                new TouchEvent(xs.get(slot), ys.get(slot), timestamp,
                                        pressure, touchMajor, touchMinor, ids.get(slot)));

                        //printMaps();

                        return DOWN;
                    } else if (ids.get(slot) != null && ids.get(slot) == identifier) {
                        tou.clear();

                        coordinateCheck();

                      /*  Log.d("TbbDatabaseHelper", "SYN_REPORT: MOVE identifier:" + ids.get(slot) + " slot:" + slot +
                                " x:" + xs.get(slot) + " y:" + ys.get(slot) + " timestamp:" + timestamp +
                                " pressure:" + pressure + " touchMajor:" + touchMajor +
                                " touchMinor:" + touchMinor + ".");*/

                        tou.put(ids.get(slot),
                                new TouchEvent(xs.get(slot), ys.get(slot),
                                        timestamp, pressure, touchMajor,
                                        touchMinor, ids.get(slot)));

                        //printMaps();

                        return MOVE;

                    }


                } else if (identifier == -1) {
                    if (ids.containsKey(slot)) {
                      /*  Log.d("TbbDatabaseHelper", "SYN_REPORT: UP identifier:" + ids.get(slot) + " slot:" + slot +
                                " x:" + xs.get(slot) + " y:" + ys.get(slot) + " timestamp:" + timestamp +
                                " pressure:" + pressure + " touchMajor:" + touchMajor +
                                " touchMinor:" + touchMinor + ".");*/

                        tou.clear();

                        tou.put(ids.get(slot),
                                new TouchEvent(xs.get(slot), ys.get(slot),
                                        timestamp, pressure, touchMajor,
                                        touchMinor, ids.get(slot)));

                        ids.remove(slot);
                        //xs.remove(slot);
                        //ys.remove(slot);

                        wasUP = true;

                        //printMaps();

                        return UP;
                    }
                }


                break;


        }

        //Log.d(LT, "t:" + type + " c:" + code + " v:" + value);
        return -1;
    }

    private void coordinateCheck() {
        if (x > -1) {
            xs.put(slot, x);
            x = -1;
        }
        if (y > -1) {
            ys.put(slot, y);
            y = -1;
        }
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
        //tou always only has one entry
        TouchEvent te = null;
        for (Map.Entry<Integer, TouchEvent> entry : tou.entrySet()) {
            te = entry.getValue();
        }
        return te;
    }
}