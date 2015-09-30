package tbb.touch;


/**
 * Process touch event received, identify type of touch
 * 
 * Feed all the touch positions via store(), when it returns true a type of
 * touch has finished. Use identifyTouch() to get the type of touch iden
 * 
 * @author Andre Rodrigues
 * 
 */
public class TPRNexusS extends TouchRecognizer {

	private final String LT = "TouchRecS";
	
	/**
	 * Identify type of touch (slide/touch/long press)
	 * 
	 * @return
	 */
	private int identifyTouch() {
		double distance = 0;
		if (touches.size() < 5) {
			touches.clear();
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
			return LONGPRESS;
		}
	}

	/**
	 * Register in array all touch positions until finger release
	 * 
	 * @return (-1) -> no touch identified yet (0) -> touch identified (1) ->
	 *         slide identified (2) -> LongPress identified
	 */
	@Override
	public int identifyOnRelease(int type, int code, int value, int timestamp) {
		// Log.d(LT,"t:"+ type+ " c:" + code + " v:"+ value);
		if(code==ABS_MT_TRACKING_ID){
			identifier=value;
		}else
		if (code == ABS_MT_PRESSURE)
			pressure = value;
		else if (code == ABS_MT_TOUCH_MAJOR)
			touchMajor = value;
		if (code == ABS_MT_POSITION_X)
			lastX = value;
		else if (code == ABS_MT_POSITION_Y)
			lastY = value;
		else if (code == SYN_MT_REPORT && value == 0) {
			TouchEvent p = new TouchEvent(lastX, lastY, timestamp, pressure, touchMajor,-1,identifier );
			touches.add(p);
		}
		if (code == SYN_MT_REPORT && lastEventCode == SYN_REPORT && touches.size() > 0) {
			lastEventCode = -1;
			//prevents double tap
			if((lastTouch-timestamp)<-doubleTapThreshold){
				//Log.d(LT, "last time:"+lastTouch+" timestamp:" +timestamp + " diference" + (lastTouch-timestamp));
				lastTouch=timestamp;
				return identifyTouch();
			}
			else
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
	public int identifyOnChange(int type, int code, int value , int timestamp){
		
		switch(code){
			case ABS_MT_POSITION_X:
				lastX=value;
				break;
			case ABS_MT_POSITION_Y:
				lastY=value;
				break;
			case ABS_MT_PRESSURE:
				pressure=value;
				break;
			case ABS_MT_TOUCH_MAJOR:
				touchMajor=value;
				break;
			case ABS_MT_TRACKING_ID:
				identifier=value;			
				break;
			case SYN_MT_REPORT:
				TouchEvent p = new TouchEvent(lastX, lastY, timestamp, pressure, touchMajor,-1,identifier);
				touches.add(p);
				
				//register touch id (alive), to detect ups on multi touch
				id_touches.add(identifier);
				
				//all fingers up
				if(lastEventCode== SYN_REPORT){
					lastEventCode = -1;
					touches.clear();
					numberTouches=0;
					biggestIdentifier=0;
					idFingerUp=0;
					return UP;  
				}
				//first down touch
				if(touches.size()<2){
					lastEventCode = -1;
					return DOWN;
				}
				//down if another finger is used
				if(identifier> biggestIdentifier ) {
					biggestIdentifier=identifier;
					lastEventCode = -1;
					numberTouches++;
					return DOWN;
				}
				//Log.d(LT,"touches size:" + touches.size()+ " num:" + numberTouches + " idTouches:" +id_touches.toString());
				
				//check if the touch event moved
				if(checkIfMoved(p)){
					lastEventCode=-1;
					return MOVE;
				}
				break;
			case SYN_REPORT:
				if(id_touches.size()<numberTouches+1){
					//Log.d(LT, " Syn report up");

					boolean fingerSet=false;
					for(int i =0;i<id_touches.size();i++){
						if(id_touches.get(i)!=i){
							idFingerUp=i;
							fingerSet=true;
						}
					}
					if(!fingerSet)
						idFingerUp=id_touches.size();
					id_touches.clear();
					numberTouches--;
					
					//added for screen reader
					clearTouchesFromId(idFingerUp);
					
					return UP;
				}
				id_touches.clear();
				break;
		}
		 
		lastEventCode = code;
		
		//Log.d(LT, "t:" + type + " c:" + code + " v:" + value);
		return -1;
	}
	
	private void clearTouchesFromId(int idFingerUp2) {
		// TODO Auto-generated method stub
		
	}
	








}
