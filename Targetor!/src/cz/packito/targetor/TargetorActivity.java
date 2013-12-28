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
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public class TargetorActivity extends Activity {

	private TargetorView theView;

	public SharedPreferences preferences;

	public static final String TARGETOR_KEY_SOUND_ON = "sound_on";
	public static final String SHARED_PREFERENCES = "TargetorPreferences";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

		theView = new TargetorView(this);
		theView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		setContentView(theView);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (theView.soundOn)
			theView.startMusic();
	}

	@Override
	protected void onStop() {
		theView.stopMusic();
		super.onStop();
	}
}
