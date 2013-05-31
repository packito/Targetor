package cz.packito.targetor;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothSocket;
import android.graphics.Typeface;
import android.widget.TextView;
/**
 * Class containing specific constants for Targetor! and objects common for more activities
 * @author packito
 *
 */
public class TargetorApplication extends Application {

	public static final String TARGETOR_EXTRA_MULTIPLAYER = "multiplayer";
	public static final String TARGETOR_KEY_SOUND_ON = "sound_on";
	public static final String SHARED_PREFERENCES = "TargetorPreferences";
	public BluetoothSocket btSocket;

	/**
	 * Change typeface to my cool downloaded font
	 * 
	 * @param activity
	 *            the actvity asking for the change (this)
	 * @param ids
	 *            the id's of the TextViews to change their typeface
	 */

	public static void changeTypeface(Activity activity, int... ids) {
		Typeface typeface = Typeface.createFromAsset(activity.getAssets(),
				"zekton__.ttf");
		for (int id : ids) {
			((TextView) activity.findViewById(id)).setTypeface(typeface);
		}
	}


}
