package tbb.interfaces;

import android.view.accessibility.AccessibilityEvent;

public interface AccessibilityEventReceiver {

	/**
	 * Receives arrayList with the updated content
	 * 
	 * @param event
	 */
	public abstract void onUpdateAccessibilityEvent(AccessibilityEvent event);
	
	public abstract  int [] getType();
}
