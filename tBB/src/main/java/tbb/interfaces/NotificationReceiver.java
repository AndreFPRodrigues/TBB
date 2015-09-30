package tbb.interfaces;

public interface NotificationReceiver {

	/**
	 * Receives notification
	 * 
	 * @param notification text
	 */
	public abstract void onNotification(String notification);

}
