package tbb.core.logger;

import java.security.GeneralSecurityException;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import tbb.core.service.TBBService;

class AccessibilityScrapping {
	private static final int THRESHOLD=100;

	public static int hashIt(AccessibilityNodeInfo n) {
		if (n != null) {
			String s = "" + n.getPackageName() + n.getClassName() + n.getText()
					+ n.getContentDescription() + n.getActions();
			return s.hashCode();
		}
		return -1;
	}

	static String getCurrentActivityName(Context context) {
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		if (activityManager == null)
			return "";
		ActivityManager.RunningTaskInfo info = activityManager.getRunningTasks(
				1).get(0);
		ComponentName topActivity = info.topActivity;
		return topActivity.getClassName();
	}

	/**
	 * Get root parent from node source
	 * 
	 * @param source
	 * @return
	 */
	static AccessibilityNodeInfo getRootParent(AccessibilityNodeInfo source) {
		AccessibilityNodeInfo current = source;
		if (current != null)
			while (current.getParent() != null) {
				AccessibilityNodeInfo oldCurrent = current;
				current = current.getParent();
				oldCurrent.recycle();
			}
		return current;
	}

	static   String cleanText(String text) {
		String  result = text.replaceAll("\""," ");
		result = result.replaceAll("\'"," ");
		result = result.replaceAll("[\r\n]","\\n");
		result = result.substring(0, Math.min(result.length(), THRESHOLD));
		return result;
	}

	static String getDescriptionNew(AccessibilityNodeInfo src) {
		try {
			if (src != null) {
				String text;
				if ((text = getText(src)) != null) {
					return cleanText(text);
				}
				else {
					int numchild = src.getChildCount();
					for (int i = 0; i < numchild; i++) {
						if (src.getChild(i)!=null && (text = getText(src.getChild(i))) != null) {
							return  cleanText(text);
						} else {
							src.getChild(i).recycle();
						}
					}
					src = src.getParent();
					numchild = src.getChildCount();
					for (int i = 0; i < numchild; i++) {
						if (src.getChild(i)!=null &&(text = getText(src.getChild(i))) != null) {
							return cleanText(text);
						} else {
							src.getChild(i).recycle();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

			return "";
		}

		return "";
	}

	/**
	 * Gets the node text either getText() or contentDescription
	 *
	 * @param src
	 * @return node text/description null if it doesnt have
	 */
	public static String getText(AccessibilityNodeInfo src) {
		String text = null;

		if (src.getText() != null || src.getContentDescription() != null) {
			if (src.getText() != null)
				text = src.getText().toString();
			else
				text = src.getContentDescription().toString();
			src.recycle();
		}

		return text;
	}


	public static String getEventText(AccessibilityEvent event){
		String step="";
		for (CharSequence cs : event.getText()) {
			step += cs + ";";
		}
		return cleanText(step);
	}
}
