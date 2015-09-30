package tbb.interfaces;

public interface IOEventReceiver {

	/**
	 * Receives io event
	 * 
	 * @param device
	 * @param type
	 * @param code
	 * @param value
	 * @param timestamp
	 */
	public abstract void onUpdateIOEvent(int device, int type, int code, int value,
                                         int timestamp);

	public abstract void onTouchReceived(int type);
}
