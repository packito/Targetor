package cz.packito.targetor;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class TargetorActivity extends Activity {

	private static final String TAG = "TargetorActivity";

	private TargetorView theView;

	public SharedPreferences preferences;

	private boolean onTop;

	public static final String TARGETOR_KEY_SOUND_ON = "sound_on";
	public static final String SHARED_PREFERENCES = "TargetorPreferences";

	private static Typeface TYPEFACE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
		TYPEFACE = Typeface.createFromAsset(getAssets(), "zekton__.ttf");

		// get screen size
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		theView = new TargetorView(this, metrics.widthPixels,
				metrics.heightPixels);
		theView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		setContentView(theView);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		onTop=true;
		theView.onResume();
	}
	@Override
	protected void onPause() {
		onTop=false;
		theView.onPause();
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		theView.onStop();
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		theView.onBackPressed();
	}

	public static Typeface getTypeface() {
		if (TYPEFACE == null) {
			Log.e(TAG, "Targetor typeface not initialised");
			return Typeface.DEFAULT;
		} else
			return TYPEFACE;
	}

	public boolean isOnTop() {
		return onTop;
	}
}
