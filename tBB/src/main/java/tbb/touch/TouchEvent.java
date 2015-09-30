package tbb.touch;

/**
 * Class that represents a touch event
 * contains coords and timestamp
 * @author unzi
 *
 */
public class TouchEvent {
	private int x;
	private int y;
	private int msec;
	private int pressure;  
	private int touchMajor;
	private int touchMinor;

	private int identifier;
	
	public TouchEvent(int x, int y, int msec){
		this.x=x;
		this.y=y;
		this.msec=msec;
	}
	
	public TouchEvent(int x, int y, double identifier){
		this.x=x;
		this.y=y;
		this.identifier=(int) identifier;
	}
	
	public TouchEvent(int x, int y, int msec, int pressure, int touchMajor, int touchMinor, int identifier){
		this.x=x;
		this.y=y;
		this.msec=msec;
		this.pressure=pressure;
		this.touchMajor = touchMajor;
		this.touchMinor= touchMinor;
		this.identifier = identifier;
	}
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public int getTime(){
		return msec;
	}

	public int getTouchMajor() {
		return touchMajor;
	}


	public int getIdentifier() {
		return identifier;
	}

	public int getPressure() {
		return pressure;
	}

	public int getTouchMinor() {
		return touchMinor;
	}


	
}
