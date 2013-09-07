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
	public static final String TARGETOR_EXTRA_SCORE = "score";
	public static final String TARGETOR_EXTRA_TARGETS_SHOT= "targets_shot";
	public static final String TARGETOR_EXTRA_MISSES = "misses";
	public static final String TARGETOR_EXTRA_LEVEL_ID= "level_id";
	public static final String TARGETOR_EXTRA_OPPONENT_SCORE = "opponent_score";
	public static final String TARGETOR_EXTRA_OPPONENT_TARGETS_SHOT= "opponent_targets_shot";
	public static final String TARGETOR_EXTRA_OPPONENT_MISSES= "opponent_misses";

	public static final String TARGETOR_KEY_SOUND_ON = "sound_on";
	public static final String SHARED_PREFERENCES = "TargetorPreferences";
	
	public static final int LEVELS=20;
	
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

	/**
	 * Change typeface to my cool downloaded font
	 * 
	 * @param activity
	 *            the actvity asking for the change (this)
	 * @param textView
	 *            the TextViews to change their typeface
	 */

	public static void changeTypeface(Activity activity,TextView... textViews) {
		Typeface typeface = Typeface.createFromAsset(activity.getAssets(),
				"zekton__.ttf");
		for (TextView textView : textViews) {
			textView.setTypeface(typeface);
		}
	}


	/**
	 * 
	 * @param lvl
	 *            the current game level (0 for multiplayer)
	 * @return the time of game for the current level, in milliseconds
	 */
	public static int calcTime(int lvl) {
		if (lvl == 0)
			return 60000;
		else
			return 5000 * (lvl + 4);
	}

	/**
	 * 
	 * @param lvl
	 *            the current game level (0 for multiplayer)
	 * @return the target for the current level, has no effect in multiplayer
	 */
	public static int calcScore(int lvl) {
		if (lvl == 0)
			return 0;
		else
			return 25 * (lvl + 4) + (int) Math.exp(lvl / 3.0);
	}

	/**
	 * 
	 * @param lvl
	 *            the current game level (0 for multiplayer)
	 * @return probability that a Normal target is created each frame
	 */
	public static double calcNormal(int lvl) {
		if (lvl == 0)
			return 0.12;
		else
			return 1.0 / 12.0 + lvl * lvl / 2500.0;
	}

	/** @see #calcNormal(int) */
	public static double calcDiamond(int lvl) {
		if (lvl == 0)
			return 0.02;
		else
			return (lvl - 3.0) / 200.0;
	}

	/** @see #calcNormal(int) */
	public static double calcFlower(int lvl) {
		if (lvl == 0)
			return 0.03;
		else
			return Math.pow(lvl, 1.5) / 600.0;
	}


}
