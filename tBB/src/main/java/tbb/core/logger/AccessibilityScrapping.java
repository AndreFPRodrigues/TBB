package tbb.core.logger;

import java.security.GeneralSecurityException;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

class AccessibilityScrapping {

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

	static String getChildren(AccessibilityNodeInfo node, int childLevel) {
		StringBuilder sb = new StringBuilder();
		if (node.getChildCount() > 0) {
			sb.append("{");
			for (int i = 0; i < node.getChildCount(); i++) {
				if (node.getChild(i) != null) {
					if (i > 0)
						sb.append("!_!");
					sb.append(getDescription(node.getChild(i)));

					if (node.getChild(i).getChildCount() > 0)
						sb.append(getChildren(node.getChild(i), childLevel + 1));
				}

			}
			sb.append("}");
		}

		return sb.toString();
	}

	static String getDescription(AccessibilityNodeInfo n) {

		// if (n.getText() != null)
		// return hashIt(n) + "," + n.getText();
		// else
		// return hashIt(n) + "," + n.getContentDescription()
		String allText = n.toString();

		// String[] unhandled = n.toString().split(";");
		String text = "" + n.getText();
		String description = "" + n.getContentDescription();
		String textE = "null";
		String descE = "null";

		// adding delimiter for easy encryption detection
		if (!text.equals(textE)) {
			textE = "!_te_!" + text + "!_te_!";
			textE = textE.replaceAll("[\n\r]", "");

		}
		if (!description.equalsIgnoreCase(descE)) {
			descE = "!_te_!" + description + "!_te_!";
			descE = descE.replaceAll("[\n\r]", "");

		}
		allText = allText.replace(text, textE);
		allText = allText.replace(description, descE);
		allText = allText.replaceAll(";", "!_!");

		// DECRYPTION
		// Encryption.decrypt( Base64.decodeBase64(test)));
		/*
		 * String result = unhandled[0]; for (int i = 1; i < 5; i++) { result +=
		 * "!_!" + unhandled[i]; } result += "!_! text: " + textE; result +=
		 * "!_! desc: " + descE; for(int i
		 * =unhandled.length-11;i<unhandled.length;i++){ result += "!_!" +
		 * unhandled[i]; }
		 */
		// Log.d("gcm", "TExt : " +allText);
		return hashIt(n) + "!_!" + allText;
	}
}
